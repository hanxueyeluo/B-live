package com.easylive.mapper;

import com.easylive.entity.po.VideoInfoPost;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频文件信息 数据库操作接口
 */
public interface VideoInfoFilePostMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据FileId更新
	 */
	 Integer updateByFileId(@Param("bean") T t,@Param("fileId") String fileId);


	/**
	 * 根据FileId删除
	 */
	 Integer deleteByFileId(@Param("fileId") String fileId);


	/**
	 * 根据FileId获取对象
	 */
	 T selectByFileId(@Param("fileId") String fileId);


	/**
	 * 根据UploadId更新
	 */
	 Integer updateByUploadId(@Param("bean") T t,@Param("uploadId") String uploadId);


	/**
	 * 根据UploadId删除
	 */
	 Integer deleteByUploadId(@Param("uploadId") String uploadId);


	/**
	 * 根据UploadId获取对象
	 */
	 T selectByUploadId(@Param("uploadId") String uploadId);


    void deleteBatchByFileId(@Param("delFileList") List<String> delFileList,@Param("userId") String userId);


	void updateByUploadIdAndUserId(@Param("bean") T updateFilePost,@Param("uploadId") String uploadId,@Param("userId") String userId);


	Integer sumDuration(@Param("videoId") String videoId);
}
