package com.likelion.likelionS3.post.application;

import com.likelion.likelionS3.common.exception.BusinessException;
import com.likelion.likelionS3.common.response.code.ErrorCode;
import com.likelion.likelionS3.image.S3Uploader;
import com.likelion.likelionS3.member.domain.Member;
import com.likelion.likelionS3.member.domain.repository.MemberRepository;
import com.likelion.likelionS3.post.api.dto.request.PostSaveRequestDto;
import com.likelion.likelionS3.post.api.dto.request.PostUpdateRequestDto;
import com.likelion.likelionS3.post.api.dto.response.PostInfoResponseDto;
import com.likelion.likelionS3.post.domain.Post;
import com.likelion.likelionS3.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final S3Uploader s3Uploader;

    // 게시물 저장
    @Transactional
    public void postSave(PostSaveRequestDto postSaveRequestDto, MultipartFile image) {
        Member member = memberRepository.findById(postSaveRequestDto.memberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_EXCEPTION, ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + postSaveRequestDto.memberId()));

        String imageUrl = null;
        if (image != null && !image.isEmpty()){
            try {
                imageUrl = s3Uploader.upload(image);
            } catch(IOException e) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL_EXCEPTION, ErrorCode.FILE_UPLOAD_FAIL_EXCEPTION.getMessage());
            }
        }
        Post post = Post.builder()
                .title(postSaveRequestDto.title())
                .contents(postSaveRequestDto.contents())
                .member(member)
                .imageUrl(imageUrl)
                .build();

        postRepository.save(post);
    }

    // 특정 작성자가 작성한 게시글 목록을 조회
    public Page<PostInfoResponseDto> postFindMember(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_EXCEPTION, ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + memberId));

        Page<Post> posts = postRepository.findByMember(member, pageable);
        return posts.map(PostInfoResponseDto::from);
    }

    // 게시물 수정
    @Transactional
    public void postUpdate(Long postId, PostUpdateRequestDto postUpdateRequestDto, MultipartFile image)
    {
        Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION, ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));

        String imageUrl = post.getImageUrl(); // 이미지 수정을 위한 기존 이미지 URL 저장
        String oldUrl = imageUrl; // 기존 이미지 url을 한번 더 저장

        // 새 이미지가 실제로 요청에 포함되어 들어왔는지 확인하고 기존 이미지 삭제
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = s3Uploader.upload(image);
                if(oldUrl != null && !oldUrl.isEmpty()){ // 기존 url이 null이 아니고 비어있지 않은 경우
                    s3Uploader.deleteFile(oldUrl); // 예전 URL 삭제한다.
                }

            } catch(IOException e) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL_EXCEPTION, ErrorCode.FILE_UPLOAD_FAIL_EXCEPTION.getMessage());
            }
        }
        // 엔티티 수정 (변경 감지 적용)
        post.update(postUpdateRequestDto, imageUrl);

    }

    // 게시물 삭제
    @Transactional
    public void postDelete(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION, ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));
        postRepository.delete(post);
    }
}
