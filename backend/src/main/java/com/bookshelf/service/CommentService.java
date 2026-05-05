package com.bookshelf.service;

import com.bookshelf.dto.comment.CommentDTO;
import com.bookshelf.dto.comment.CreateCommentDTO;
import com.bookshelf.entity.*;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final LikeService likeService;

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
                .build();

        return toDTO(commentRepository.save(comment), userId);
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
                .build();

        return toDTO(commentRepository.save(comment), userId);
    }

    @Transactional
    public CommentDTO updateComment(UUID userId, UUID commentId, CreateCommentDTO dto) {
        Comment comment = findById(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете редактировать чужой комментарий");
        }

        comment.setText(dto.getText());
        return toDTO(commentRepository.save(comment), userId);
    }

    @Transactional
    public void deleteComment(UUID userId, UUID commentId, boolean isModerator) {
        Comment comment = findById(commentId);

        if (!isModerator && !comment.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете удалить чужой комментарий");
        }

        likeService.deleteFor(LikeService.TARGET_COMMENT, commentId);
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsForReview(UUID reviewId, UUID viewerId) {
        List<Comment> comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
        Set<UUID> likedIds = likeService.findLikedIds(viewerId, LikeService.TARGET_COMMENT,
                comments.stream().map(Comment::getId).collect(Collectors.toList()));
        return comments.stream()
                .map(c -> toDTO(c, likedIds.contains(c.getId()),
                        likeService.count(LikeService.TARGET_COMMENT, c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsForCollection(UUID collectionId, UUID viewerId) {
        List<Comment> comments = commentRepository.findByCollectionIdOrderByCreatedAtAsc(collectionId);
        Set<UUID> likedIds = likeService.findLikedIds(viewerId, LikeService.TARGET_COMMENT,
                comments.stream().map(Comment::getId).collect(Collectors.toList()));
        return comments.stream()
                .map(c -> toDTO(c, likedIds.contains(c.getId()),
                        likeService.count(LikeService.TARGET_COMMENT, c.getId())))
                .collect(Collectors.toList());
    }

    private Comment findById(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Комментарий не найден"));
    }

    public CommentDTO toDTO(Comment c, UUID viewerId) {
        long count = likeService.count(LikeService.TARGET_COMMENT, c.getId());
        boolean liked = viewerId != null && !likeService
                .findLikedIds(viewerId, LikeService.TARGET_COMMENT, List.of(c.getId())).isEmpty();
        return toDTO(c, liked, count);
    }

    public CommentDTO toDTO(Comment c, boolean liked, long likes) {
        return CommentDTO.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .userName(c.getUser().getLogin())
                .userAvatar(c.getUser().getAvatarUrl())
                .text(c.getText())
                .likes(likes)
                .liked(liked)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
