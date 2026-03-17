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
        var mechanic = mechanicRepository.findById(request.mechanicId())
                .orElseThrow(() -> new EntityNotFoundException("Mechanic with id: " + request.mechanicId()));

        var order = serviceOrderRepository.findById(request.serviceOrderId())
                .orElseThrow(() -> new EntityNotFoundException("ServiceOrder with id: " + request.serviceOrderId()));

        if (!order.status().equals(OrderStatus.COMPLETED)) {
            throw new IllegalArgumentException("Service order is not completed");
        }

        if (!order.mechanicId().equals(request.mechanicId())) {
            throw new IllegalArgumentException("This mechanic did not perform the service order");
        }

        if (reviewRepository.findByServiceOrderId(request.serviceOrderId()).isPresent()) {
            throw new IllegalArgumentException("This service order has already been reviewed");
        }

        var review = new ReviewEntity(null, request.mechanicId(), request.serviceOrderId(),
                request.rating(), request.comment(), LocalDateTime.now());
        var saved = reviewRepository.save(review);

        mechanicRepository.save(mechanic.withNewReview(request.rating()));

        return toResponse(saved);
    }

    public List<ReviewResponse> getReviewsForMechanic(String mechanicId) {
        mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException("Mechanic with id: " + mechanicId));
        return reviewRepository.findByMechanicId(mechanicId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public double getAverageRating(String mechanicId) {
        var mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException("Mechanic with id: " + mechanicId));
        return mechanic.score();
    }

    public void deleteReview(String reviewId) {
        var review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review with id: " + reviewId));

        var mechanic = mechanicRepository.findById(review.mechanicId())
                .orElseThrow(() -> new EntityNotFoundException("Mechanic with id: " + review.mechanicId()));

        reviewRepository.deleteById(reviewId);
        mechanicRepository.save(mechanic.withRemovedReview(review.rating()));
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
