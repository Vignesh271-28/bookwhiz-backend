package com.example.BookWhiz.service.review;


import java.util.List;

import com.example.BookWhiz.dto.request.ReviewRequest;
import com.example.BookWhiz.dto.response.ReviewResponse;

public interface ReviewService {

    // Anyone can read
    List<ReviewResponse> getReviewsByMovie(Long movieId);

    // Logged-in users
    ReviewResponse addReview(Long userId, ReviewRequest request);

    // Owner or admin/super_admin
    void deleteReview(Long reviewId, Long requestingUserId, String requestingUserRole);
}