package com.example.BookWhiz.service.review;

import com.example.BookWhiz.dto.request.ReviewRequest;
import com.example.BookWhiz.dto.response.ReviewResponse;
import com.example.BookWhiz.model.movie.Movie;
import com.example.BookWhiz.model.review.Review;
import com.example.BookWhiz.model.user.User;
import com.example.BookWhiz.repository.MovieRepository;
import com.example.BookWhiz.repository.ReviewRepository;
import com.example.BookWhiz.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final MovieRepository  movieRepo;
    private final UserRepository   userRepo;

    public ReviewServiceImpl(ReviewRepository reviewRepo,
                             MovieRepository  movieRepo,
                             UserRepository   userRepo) {
        this.reviewRepo = reviewRepo;
        this.movieRepo  = movieRepo;
        this.userRepo   = userRepo;
    }

    // ── GET all reviews for a movie ───────────────────────────
    @Override
    public List<ReviewResponse> getReviewsByMovie(Long movieId) {
        return reviewRepo.findByMovieIdOrderByCreatedAtDesc(movieId)
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    // ── ADD review ────────────────────────────────────────────
    @Override
    public ReviewResponse addReview(Long userId, ReviewRequest req) {

        // Validate rating range
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        // Validate comment
        if (req.getComment() == null || req.getComment().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }

        if (req.getComment().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment too long (max 500 chars)");
        }

        // Check user exists
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check movie exists
        Movie movie = movieRepo.findById(req.getMovieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        // Prevent duplicate reviews — one user, one review per movie
        reviewRepo.findByUserIdAndMovieId(userId, req.getMovieId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already reviewed this movie. Delete your previous review to submit a new one.");
        });

        Review review = new Review();
        review.setUser(user);
        review.setMovie(movie);
        review.setRating(req.getRating());
        review.setComment(req.getComment().trim());

        return ReviewResponse.from(reviewRepo.save(review));
    }

    // ── DELETE review ─────────────────────────────────────────
    @Override
    public void deleteReview(Long reviewId, Long requestingUserId, String requestingUserRole) {

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        boolean isOwner = review.getUser() != null &&
                          review.getUser().getId().equals(requestingUserId);

        boolean isAdmin = requestingUserRole != null &&
                          (requestingUserRole.contains("ADMIN") || requestingUserRole.contains("SUPER_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorised to delete this review");
        }

        reviewRepo.deleteById(reviewId);
    }
}