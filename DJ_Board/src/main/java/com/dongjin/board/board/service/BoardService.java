package com.dongjin.board.board.service;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dongjin.board.board.dto.PostDTO;
import com.dongjin.board.board.dto.RelatedPostDTO;
import com.dongjin.board.board.dto.RelatedPostFrequencyDTO;
import com.dongjin.board.board.entity.Post;
import com.dongjin.board.board.entity.RelatedPost;
import com.dongjin.board.board.entity.RelatedPostFrequency;
import com.dongjin.board.board.paging.SelectCriteria;
import com.dongjin.board.board.repository.BoardRelatedPostFrequencyRepository;
import com.dongjin.board.board.repository.BoardRelatedPostRepository;
import com.dongjin.board.board.repository.BoardRepository;

import jakarta.transaction.Transactional;

@Service
public class BoardService {

	private static final Logger log = LoggerFactory.getLogger(BoardService.class);
	private final ModelMapper modelMapper;
	private final BoardRepository boardRepository;
	private final BoardRelatedPostRepository boardRelatedPostRepository;
	private final BoardRelatedPostFrequencyRepository boardRelatedPostFrequencyRepository;

	@Autowired
	public BoardService(BoardRepository boardRepository, ModelMapper modelMapper,
			BoardRelatedPostRepository boardRelatedPostRepository, BoardRelatedPostFrequencyRepository boardRelatedPostFrequencyRepository) {
		this.modelMapper = modelMapper;
		this.boardRepository = boardRepository;
		this.boardRelatedPostRepository = boardRelatedPostRepository;
		this.boardRelatedPostFrequencyRepository = boardRelatedPostFrequencyRepository;
	}

	/* 게시글 전체 조회 */
	@Transactional
	public List<PostDTO> findPostList() {

		List<Post> postList = boardRepository.findAll(Sort.by("postId"));

        log.info("[BoardService] findPostByPostId postList1 확인 : " + postList);

		return postList.stream().map(post -> modelMapper.map(post, PostDTO.class)).collect(Collectors.toList());
	}

	/* 페이징, 검색 */
	public int selectTotalCount(String searchCondition, String searchValue) {

		int count = 0;
		if (searchValue != null) {
			if ("title".equals(searchCondition)) {
				count = boardRepository.countByTitleContaining(searchValue);
			}

		} else {
			count = (int) boardRepository.count();
		}

		return count;
	}

	/* 게시글 검색 리스트 페이징 처리 포함 */
	public List<PostDTO> searchBoardList(SelectCriteria selectCriteria) {

		int index = selectCriteria.getPageNo() - 1; // Pageble객체를 사용시 페이지는 0부터 시작(1페이지가 0)
		int count = selectCriteria.getLimit();
		String searchValue = selectCriteria.getSearchValue();

		/* 페이징 처리와 정렬을 위한 객체 생성 */
		Pageable paging = PageRequest.of(index, count, Sort.by(Sort.Direction.DESC, "postId")); // Pageable은
																								// org.springframework.data.domain패키지로
																								// import

		List<Post> postList = new ArrayList<Post>();
		if (searchValue != null) {

			if ("title".equals(selectCriteria.getSearchCondition())) {
				postList = boardRepository.findByTitleContaining(selectCriteria.getSearchValue(), paging);
			}

		} else {
			postList = boardRepository.findAll(paging).toList();
		}

		/* 자바의 Stream API와 ModelMapper를 이용하여 entity를 DTO로 변환 후 List<MenuDTO>로 반환 */
		return postList.stream().map(post -> modelMapper.map(post, PostDTO.class)).collect(Collectors.toList());
	}

	/* 상세페이지 */
	@Transactional
	public PostDTO findPostById(int postId) {
		log.info("[BoardService] findPostById Start ==================");
		Post post = boardRepository.findById(postId).get();

		log.info("[BoardService] findPostById test : " + post);

		log.info("[BoardService] findPostById End ==================");
		return modelMapper.map(post, PostDTO.class);
	}

	@Transactional
	public void registNewPost(PostDTO newPost, RelatedPostDTO newRelatedPost, RelatedPostFrequencyDTO newRelatedPostFrequency) {
		log.info("[BoardService] registNewPost Start ==================");
		
		/* 게시물 DB를 위한 저장 */
	    java.util.Date now = new java.util.Date(); // 현재 시간 구하기
	    java.sql.Date sqlDate = new java.sql.Date(now.getTime()); // java.util.Date를 java.sql.Date로 변환
	    newPost.setCreatedAT(sqlDate); // 현재 시간 저장

	    Post post = modelMapper.map(newPost, Post.class);
	    
	    boardRepository.save(post);
	    
	    
	    /* 연관 게시물 테이블 저장 */
	    Integer postId = post.getPostId(); // 새로 생성된 게시물의 postId 값 가져오기
		
		log.info("[BoardService] registNewPost postId 확인 : " + postId);

	    
	    String[] words = post.getContent().split("\\s+");

	    Map<String, Integer> wordCounts = new HashMap<>();
	    for (String word : words) {
	        if (wordCounts.containsKey(word)) {
	            wordCounts.put(word, wordCounts.get(word) + 1);
	        } else {
	            wordCounts.put(word, 1);
	        }
	    }
	    
		log.info("[BoardService] registNewPost 게시물 내용 단어 별 횟수 카운팅 : " + wordCounts);

	    long totalCount = words.length;

		log.info("[BoardService] registNewPost totalCount : " + totalCount);

	    List<String> frequentWords = new ArrayList<>();
	    for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
	    	String word = entry.getKey();
	        int count = entry.getValue();
	    	
	        log.info("[BoardService] registNewPost 새로 등록되는 단어 확인 : " + word);

	        double frequency = (double) count / (double) totalCount;
	        
	        log.info("[BoardService] registNewPost frequency : " + frequency);
	       

	        // 빈도 수 60% 이하 단어만 포함
	        if (frequency < 0.6) {
	        	
    	        log.info("[BoardService] registNewPost word 확인 : " + word);
  	
    	        // 중복 확인용
	        	RelatedPost existingRelatedPost = boardRelatedPostRepository.findByWord(word);
    	        log.info("[BoardService] registNewPost existingRelatedPost 확인 : " + existingRelatedPost);


    	        // 만약 이미 등록된 단어인 경우 if문 실행
	            if (existingRelatedPost != null) {
	            	
	            	log.info("[BoardService] registNewPost existingRelatedPost if 문 Start ========= ");
	            	
	            	log.info("[BoardService] registNewPost 이전에 존재하던 단어 word 확인 : " + word);
	    	        
	    	        // 이미 등록된 단어의 PK값
	            	int existingRelatedPostId = existingRelatedPost.getRelatedPostId();
	    	        log.info("[BoardService] registNewPost existingRelatedPostId 확인 : " + existingRelatedPostId);
	                	    	       
	    	        // 이미 등록되어 있는 단어의 경우이기 때문에 RelatedPost에 저장할 필요 없음
	    	        newRelatedPostFrequency.setId(0); // 시퀀스에서 가져온 다음 값 설정
	                newRelatedPostFrequency.setPostId(postId);
	                newRelatedPostFrequency.setRelatedPostId(existingRelatedPostId);
	                newRelatedPostFrequency.setFrequency(count);
	    	        	    	       
	    	        RelatedPostFrequency relatedPostFrequency = modelMapper.map(newRelatedPostFrequency, RelatedPostFrequency.class);
	    	        boardRelatedPostFrequencyRepository.save(relatedPostFrequency);
	    	        log.info("[BoardService] registNewPost 저장되는 데이터 확인 relatedPostFrequency : " + relatedPostFrequency);

	    	        log.info("[BoardService] registNewPost existingRelatedPost if 문 End ========= ");

	    	        
	            } else {
	            	
	    	        log.info("[BoardService] registNewPost existingRelatedPost if 문 else Start ========= ");
	            	
	                // 등록된 단어가 없을 경우, 시퀀스에서 새로운 PK값을 가져옴       

	    	        log.info("[BoardService] registNewPost 새로 등록되는 단어 확인 : " + word);
	    	        
	    	        // 새로운 단어 등록
	    	        newRelatedPost.setRelatedPostId(0); // 시퀀스에서 가져온 다음 값 설정
	    	        newRelatedPost.setWord(word);
	    	        RelatedPost relatedPost = modelMapper.map(newRelatedPost, RelatedPost.class);
	    	        boardRelatedPostRepository.save(relatedPost);

	    	        log.info("[BoardService] registNewPost 저장되는 relatedPost 확인 : " + relatedPost);

	    	        Integer relatedPostId = relatedPost.getRelatedPostId(); // 새로 생성된 게시물의 relatedPostId 값 가져오기

	    	        log.info("[BoardService] registNewPost 새로 생성된 relatedPostId 가져오나 : " + relatedPostId);

	    	        // 게시물마다 등록될 단어 저장
	    	        newRelatedPostFrequency.setId(0); // 시퀀스에서 가져온 다음 값 설정
	                newRelatedPostFrequency.setPostId(postId);
	                newRelatedPostFrequency.setRelatedPostId(relatedPostId);
	                newRelatedPostFrequency.setFrequency(count);
	                
	    	        RelatedPostFrequency relatedPostFrequency = modelMapper.map(newRelatedPostFrequency, RelatedPostFrequency.class);
	    	        boardRelatedPostFrequencyRepository.save(relatedPostFrequency);
	    	        
	    	        log.info("[BoardService] registNewPost 저장되는 relatedPostFrequency 확인 : " + relatedPostFrequency);

	    	        log.info("[BoardService] registNewPost existingRelatedPost if 문 else End ========= ");

	            }
	        

	        }
	        // frequentWords 리스트 초기화
	        frequentWords.clear();
	        log.info("[BoardService] registNewPost frequentWords.clear() 확인 : ");

	    }
	   

		log.info("[BoardService] registNewPost End ==================");
	}

	/* 연관 게시물 추천 */
	@Transactional
	public List<PostDTO> findRelatedPostListById(int postId) {
		
		log.info("[BoardService] findPostByPostId Start ==================");
		
		// 게시물 등록된 단어 컬럼 검색
		List<RelatedPostFrequency> relatedPostFrequency = boardRelatedPostFrequencyRepository.findByPostId(postId);
		
		log.info("[BoardService] findPostByPostId relatedPostFrequency 확인 : " + relatedPostFrequency);

		// 등록된 단어의 relatedPostId를 for문을 통해 추출
		List<Integer> relatedPostIdList = new ArrayList<>();
		for (RelatedPostFrequency rpf : relatedPostFrequency) {
		    relatedPostIdList.add(rpf.getRelatedPostId());
		}
		log.info("[BoardService] findPostByPostId relatedPostIdList 확인 : " + relatedPostIdList);
		
			// 추출한 Id를 통해 같은 단어를 사용한 게시물 연관테이블에서 검색
			List<Post> relatedPostList = new ArrayList<>();
		    for (int relatedPostId : relatedPostIdList) {
		    	
		    		
		    	
		    	// 선택한 게시물의 postId 구하기 
		        List<RelatedPostFrequency> findRelatedPost = boardRelatedPostFrequencyRepository.findPostIdByRelatedPostId(relatedPostId);
		       
		        // 빈도 수 기준으로 정렬된 리스트 구하기
		        List<RelatedPostFrequency> sortedRelatedPostFrequencyList = findRelatedPost.stream()
		                .filter(relatedPost -> relatedPost.getPostId() != postId)
		                .sorted(Comparator.comparingInt(RelatedPostFrequency::getFrequency).reversed())
		                .collect(Collectors.toList());
	
		        log.info("[BoardService] findPostByPostId findRelatedPost 확인 : " + findRelatedPost);
	
		        // 검색한 데이터(findRelatedPost에서 postId만 추출해서, boardRepository에 findById(findRelatedPost)의 형식으로 List 뽑기
		        List<Integer> postIdList = sortedRelatedPostFrequencyList.stream()
		                .limit(3)
		                .map(RelatedPostFrequency::getPostId)
		                .collect(Collectors.toList());
	
	
		        log.info("[BoardService] findPostByPostId postIdList 확인 : " + postIdList);
		        
		        // 빈도 수 내림차 순으로 얻은 postId로 게시물 리스트 만들기
		        // 빈도 수 높은 순서대로 나열하기 위해 List에 담긴 값을 따로 쿼리 메소드 실행
		        List<Post> relatedPostList1 = new ArrayList<>();
		        for (int postId1 : postIdList) {
		            Optional<Post> post = boardRepository.findById(postId1);
		            post.ifPresent(relatedPostList1::add);
		        }
		        relatedPostList.addAll(relatedPostList1);
		        
		        log.info("[BoardService] findPostByPostId relatedPostList 확인 : " + relatedPostList1);
		        
		    }
		    // 3개 미만의 연관 게시물만 나오게 설정
		    List<Post> postList;
		    if (relatedPostList.size() <= 3) {
		        postList = relatedPostList;
		    } else {
		        postList = relatedPostList.subList(0, 3);
		    }

		    log.info("[BoardService] findPostByPostId postList2 확인 : " + postList);
		    log.info("[BoardService] findPostByPostId if End ==================");
		    return postList.stream().map(post -> modelMapper.map(post,PostDTO.class)).collect(Collectors.toList());
    }
	  
}



	
	

