// package com.example.BookWhiz.service.analytics;

// import com.example.BookWhiz.dto.response.DashboardStatsResponse;
// import com.example.BookWhiz.model.booking.BookingStatus;
// import com.example.BookWhiz.model.payment.Payment;
// import com.example.BookWhiz.model.payment.PaymentStatus;
// import com.example.BookWhiz.repository.BookingRepository;
// import com.example.BookWhiz.repository.EventRepository;
// import com.example.BookWhiz.repository.PaymentRepository;
// import org.springframework.stereotype.Service;

// import java.util.HashMap;
// import java.util.Map;

// @Service
// public class AnalyticsServiceImpl implements AnalyticsService {

//     private final BookingRepository bookingRepository;
//     private final PaymentRepository paymentRepository;
//     private final EventRepository eventRepository;

//     public AnalyticsServiceImpl(
//             BookingRepository bookingRepository,
//             PaymentRepository paymentRepository,
//             EventRepository eventRepository
//     ) {
//         this.bookingRepository = bookingRepository;
//         this.paymentRepository = paymentRepository;
//         this.eventRepository = eventRepository;
//     }

//     @Override
//     public DashboardStatsResponse getPlatformStats() {

//         long totalBookings = bookingRepository.count();
//         long totalEvents = eventRepository.count();

//         double totalRevenue = paymentRepository.findAll()
//                 .stream()
//                 .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
//                 .mapToDouble(Payment::getAmount)
//                 .sum();

//         return new DashboardStatsResponse(
//                 totalBookings,
//                 totalRevenue,
//                 totalEvents, totalEvents, totalEvents, totalRevenue, totalRevenue, totalRevenue, totalEvents, totalEvents, totalEvents
//         );
//     }

//     @Override
//     public Map<String, Object> getAllUsersAnalytics() {

//         long totalBookings = bookingRepository.count();

//         long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
//         long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);

//         double totalRevenue = paymentRepository.findAll()
//                 .stream()
//                 .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
//                 .mapToDouble(Payment::getAmount)
//                 .sum();

//         Map<String, Object> analytics = new HashMap<>();
//         analytics.put("totalBookings", totalBookings);
//         analytics.put("confirmedBookings", confirmedBookings);
//         analytics.put("cancelledBookings", cancelledBookings);
//         analytics.put("totalRevenue", totalRevenue);

//         return analytics;
//     }
// }