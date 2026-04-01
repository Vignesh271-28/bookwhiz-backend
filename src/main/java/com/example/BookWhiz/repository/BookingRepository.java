package com.example.BookWhiz.repository;

import com.example.BookWhiz.model.booking.Booking;
import com.example.BookWhiz.model.booking.BookingStatus;
import com.example.BookWhiz.model.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT s.id FROM Booking b JOIN b.seats s WHERE b.show.id = :showId AND b.status = :status")
    List<Long> findBookedSeatIdsByShowIdAndStatus(
            @Param("showId") Long showId,
            @Param("status") BookingStatus status
    );

    List<Booking> findByStatusAndLockedAtBefore(
            BookingStatus status, LocalDateTime time);

        List<Booking> findByUser(User user);

        long countByStatus(BookingStatus status);

  

@Query("SELECT b.id FROM Booking b WHERE b.show.id IN :showIds")
    List<Long> findIdsByShowIdIn(@Param("showIds") List<Long> showIds);

    // ── Deletes all bookings for given show IDs
    @Modifying
    @Query("DELETE FROM Booking b WHERE b.show.id IN :showIds")
    void deleteByShowIdIn(@Param("showIds") List<Long> showIds);

}

