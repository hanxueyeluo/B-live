package com.easylive.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;


import com.easylive.component.RabbitMQComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.*;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.query.VideoInfoFilePostQuery;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.VideoInfoFilePostMapper;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mapper.VideoInfoPostMapper;
import com.easylive.service.VideoInfoPostService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 视频信息 业务接口实现
 */
@Service("videoInfoPostService")
public class VideoInfoPostServiceImpl implements VideoInfoPostService {

	@Resource
	private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost,VideoInfoFilePostQuery> videoInfoFilePostMapper;

	@Resource
	private RabbitMQComponent rabbitMQComponent;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoInfoPost> findListByParam(VideoInfoPostQuery param) {
		return this.videoInfoPostMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoInfoPostQuery param) {
		return this.videoInfoPostMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoInfoPost> list = this.findListByParam(param);
		PaginationResultVO<VideoInfoPost> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoInfoPost bean) {
		return this.videoInfoPostMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoPostMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoPostMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoInfoPost bean, VideoInfoPostQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoPostMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoInfoPostQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoPostMapper.deleteByParam(param);
	}

	/**
	 * 根据VideoId获取对象
	 */
	@Override
	public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
		return this.videoInfoPostMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId修改
	 */
	@Override
	public Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId) {
		return this.videoInfoPostMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	@Override
	public Integer deleteVideoInfoPostByVideoId(String videoId) {
		return this.videoInfoPostMapper.deleteByVideoId(videoId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> filePostList) {
 		if (filePostList.size()>redisComponent.getSysSettingDto().getVideoCount()){
			 throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		 if (!StringTools.isEmpty(videoInfoPost.getVideoId())){
			 VideoInfoPost videoInfoDb=this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
			 if (videoInfoDb == null) {
				 throw new BusinessException(ResponseCodeEnum.CODE_600);
			 }
			 if (ArrayUtils.contains(new Integer[]{VideoStatusEnum.STATUS0.getStatus(),VideoStatusEnum.STATUS2.getStatus()},videoInfoDb.getStatus())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			 }
		 }
		 Date curDate=new Date();
		 String videoId=videoInfoPost.getVideoId();
		 List<VideoInfoFilePost> deleteFileList=new ArrayList<>();
		 List<VideoInfoFilePost> addFileList=filePostList;

		 //如果id为空那么就是上传，如果id不为空那么就是修改
		if (StringTools.isEmpty(videoId)) {
			//插入
			videoId=StringTools.getRandomString(Constants.LENGTH_10);
			videoInfoPost.setVideoId(videoId);
			videoInfoPost.setCreateTime(curDate);
			videoInfoPost.setLastUpdateTime(curDate);
			videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
			this.videoInfoPostMapper.insert(videoInfoPost);
		}else{
			//修改信息
			VideoInfoFilePostQuery filePostQuery=new VideoInfoFilePostQuery();
			filePostQuery.setVideoId(videoId);
			filePostQuery.setUserId(videoInfoPost.getUserId());
			List<VideoInfoFilePost> dbInfoFileInfo=this.videoInfoFilePostMapper.selectList(filePostQuery);
			Map<String,VideoInfoFilePost> uploadFileMap=filePostList.stream().collect(Collectors.toMap(item->item.getUploadId(), Function.identity(),
					(data1,data2)->data2));

			//判断上传的文件名称是否被修改，如果被修改则将其标记
			Boolean updateFileName=false;
			for(VideoInfoFilePost filePost:dbInfoFileInfo){
				VideoInfoFilePost updateFile=uploadFileMap.get(filePost.getUploadId());
				if (updateFile == null) {
					deleteFileList.add(filePost);
				} else if (!updateFile.getFileName().equals(filePost.getFileName())) {
					updateFileName=true;
				}
			}
			addFileList=filePostList.stream().filter(item->item.getFileName()==null).collect(Collectors.toList());
			videoInfoPost.setLastUpdateTime(curDate);
			Boolean changeVideoInfo=this.changeVideoInfo(videoInfoPost);
			//上传或修改之后将状态改为转码中和待审核
			if (addFileList!=null&&!addFileList.isEmpty()){
				videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
			} else if (changeVideoInfo||updateFileName) {
				videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
			}
			this.videoInfoPostMapper.updateByVideoId(videoInfoPost,videoInfoPost.getVideoId());
		}

		if (!deleteFileList.isEmpty()){
			List<String> delFileList=deleteFileList.stream().map(item->item.getFileId()).collect(Collectors.toList());
			this.videoInfoFilePostMapper.deleteBatchByFileId(delFileList,videoInfoPost.getUserId());
			List<String> delFilePathList=deleteFileList.stream().map(item->item.getFilePath()).collect(Collectors.toList());
			rabbitMQComponent.addFile2DelQueue(videoId,delFilePathList);
		}
		Integer index=1;
		for (VideoInfoFilePost videoInfoFilePost:filePostList){
			videoInfoFilePost.setFileIndex(index++);
			videoInfoFilePost.setVideoId(videoId);
			videoInfoFilePost.setUserId(videoInfoPost.getUserId());
			if (videoInfoFilePost.getFileId()==null){
				videoInfoFilePost.setFileId(StringTools.getRandomString(Constants.LENGTH_30));
				videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
				videoInfoFilePost.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
			}
		}
		this.videoInfoFilePostMapper.insertOrUpdateBatch(filePostList);

		if (addFileList!=null&&!addFileList.isEmpty()){
			for(VideoInfoFilePost file:addFileList){
				file.setUserId(videoInfoPost.getUserId());
				file.setVideoId(videoId);
			}
			rabbitMQComponent.addFile2TransferQueue(addFileList);
		}

	}

	private Boolean changeVideoInfo(VideoInfoPost videoInfoPost) {
		VideoInfoPost dbInfo=this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
		//判断标题，封面，标签，简介是否被修改
		return videoInfoPost.getVideoName().equals(dbInfo.getVideoName())||
				videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover())||
				videoInfoPost.getTags().equals(dbInfo.getTags())||
				videoInfoPost.getIntroduction().equals(dbInfo.getIntroduction());
	}

	@Override
	public void transferVideoFile(VideoInfoFilePost videoInfoFilePost) {

	}
}