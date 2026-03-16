package ro.unibuc.prodeng.service;

import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.model.ReviewEntity;
import ro.unibuc.prodeng.repository.MechanicRepository;
import ro.unibuc.prodeng.repository.ReviewRepository;
import ro.unibuc.prodeng.repository.ServiceOrderRepository;
import ro.unibuc.prodeng.request.CreateReviewRequest;
import ro.unibuc.prodeng.response.ReviewResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MechanicRepository mechanicRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         MechanicRepository mechanicRepository,
                         ServiceOrderRepository serviceOrderRepository) {
        this.reviewRepository = reviewRepository;
        this.mechanicRepository = mechanicRepository;
        this.serviceOrderRepository = serviceOrderRepository;
    }

    public ReviewResponse createReview(CreateReviewRequest request) {
        mechanicRepository.findById(request.mechanicId())
                .orElseThrow(() -> new EntityNotFoundException("Mechanic with id: " + request.mechanicId()));

        var order = serviceOrderRepository.findById(request.serviceOrderId())
                .orElseThrow(() -> new EntityNotFoundException("ServiceOrder with id: " + request.serviceOrderId()));

        if (!order.status().equals(OrderStatus.COMPLETED)) {
            throw new IllegalArgumentException("Service order is not completed");
        }

        if (reviewRepository.findByServiceOrderId(request.serviceOrderId()).isPresent()) {
            throw new IllegalArgumentException("This service order has already been reviewed");
        }

        var review = new ReviewEntity(null, request.mechanicId(), request.serviceOrderId(),
                request.rating(), request.comment(), LocalDateTime.now());
        return toResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getReviewsForMechanic(String mechanicId) {
        mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException("Mechanic with id: " + mechanicId));
        return reviewRepository.findByMechanicId(mechanicId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public double getAverageRating(String mechanicId) {
        var reviews = getReviewsForMechanic(mechanicId);
        if (reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToInt(ReviewResponse::rating).average().orElse(0.0);
    }

    public void deleteReview(String reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new EntityNotFoundException("Review with id: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }

    private ReviewResponse toResponse(ReviewEntity review) {
        return new ReviewResponse(
                review.id(),
                review.mechanicId(),
                review.serviceOrderId(),
                review.rating(),
                review.comment(),
                review.createdAt()
        );
    }
}
