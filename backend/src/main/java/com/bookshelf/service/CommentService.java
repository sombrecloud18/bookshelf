package com.bookshelf.service;

import com.bookshelf.dto.comment.CommentDTO;
import com.bookshelf.dto.comment.CreateCommentDTO;
import com.bookshelf.entity.*;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDTO addCommentToReview(UUID userId, UUID reviewId, CreateCommentDTO dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> AppException.notFound("Рецензия не найдена"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        Comment comment = Comment.builder()
                .review(review)
                .user(user)
                .text(dto.getText())
                .likes(0)
                .build();

        return toDTO(commentRepository.save(comment));
    }

    @Transactional
    public CommentDTO addCommentToCollection(UUID userId, UUID collectionId, CreateCommentDTO dto) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> AppException.notFound("Подборка не найдена"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        Comment comment = Comment.builder()
                .collection(collection)
                .user(user)
                .text(dto.getText())
                .likes(0)
                .build();

        return toDTO(commentRepository.save(comment));
    }

    @Transactional
    public CommentDTO updateComment(UUID userId, UUID commentId, CreateCommentDTO dto) {
        Comment comment = findById(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете редактировать чужой комментарий");
        }

        comment.setText(dto.getText());
        return toDTO(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(UUID userId, UUID commentId, boolean isModerator) {
        Comment comment = findById(commentId);

        if (!isModerator && !comment.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете удалить чужой комментарий");
        }

        commentRepository.delete(comment);
    }

    public List<CommentDTO> getCommentsForReview(UUID reviewId) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<CommentDTO> getCommentsForCollection(UUID collectionId) {
        return commentRepository.findByCollectionIdOrderByCreatedAtAsc(collectionId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void likeComment(UUID commentId) {
        Comment comment = findById(commentId);
        comment.setLikes(comment.getLikes() + 1);
        commentRepository.save(comment);
    }

    private Comment findById(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Комментарий не найден"));
    }

    public CommentDTO toDTO(Comment c) {
        return CommentDTO.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .userName(c.getUser().getLogin())
                .userAvatar(c.getUser().getAvatarUrl())
                .text(c.getText())
                .likes(c.getLikes())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
