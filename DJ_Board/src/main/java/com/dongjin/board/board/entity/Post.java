package com.dongjin.board.board.entity;

import java.sql.Clob;
import java.sql.Date;
import java.sql.Timestamp;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@EntityListeners(AuditingEntityListener.class)
@Entity(name="Post")
@Table(name = "BOARD_POST")
@SequenceGenerator( /// 식별자, 시퀀스전략
        name = "POST_SEQ_GENERATOR",
        sequenceName = "SEQ_BOARD_POST",
        initialValue = 1,
        allocationSize = 1
)
public class Post {

	@Id
	@GeneratedValue(
			strategy = GenerationType.SEQUENCE,
			generator = "POST_SEQ_GENERATOR"
			)
	@Column(name = "POST_ID")
	private int  postId;
	
	@Column(name = "CONTENT")
	private String content;
	
	@Column(name = "TITLE")
	private String title;
	
	@Column(name = "AUTHOR_ID")
	private int authorId;
	
	@Column(name = "CREATED_AT")
	private java.sql.Date createdAT;

	public Post() {
		super();
	}

	public Post(int postId, String content, String title, int authorId, Date createdAT) {
		super();
		this.postId = postId;
		this.content = content;
		this.title = title;
		this.authorId = authorId;
		this.createdAT = createdAT;
	}

	@Override
	public String toString() {
		return "Post [postId=" + postId + ", content=" + content + ", title=" + title + ", authorId=" + authorId
				+ ", createdAT=" + createdAT + "]";
	}

	public int getPostId() {
		return postId;
	}

	public void setPostId(int postId) {
		this.postId = postId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getAuthorId() {
		return authorId;
	}

	public void setAuthorId(int authorId) {
		this.authorId = authorId;
	}

	public java.sql.Date getCreatedAT() {
		return createdAT;
	}

	public void setCreatedAT(java.sql.Date createdAT) {
		this.createdAT = createdAT;
	}

	

}
