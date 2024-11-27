package com.easylive.service.impl;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;


import com.easylive.component.RabbitMQComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.UploadFileDto;
import com.easylive.entity.enums.*;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.query.VideoInfoFilePostQuery;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.VideoInfoFilePostMapper;

import com.easylive.utils.FFmpegUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import org.elasticsearch.common.recycler.Recycler;
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
@Slf4j
public class VideoInfoPostServiceImpl implements VideoInfoPostService {

	@Resource
	private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost,VideoInfoFilePostQuery> videoInfoFilePostMapper;

	@Resource
	private RabbitMQComponent rabbitMQComponent;

	@Resource
	private AppConfig appConfig;

	@Resource
	private FFmpegUtils fFmpegUtils;


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
		VideoInfoFilePost updateFilePost=new VideoInfoFilePost();
		try {
			UploadFileDto fileDto=redisComponent.getUploadVideoDto(videoInfoFilePost.getUserId(),videoInfoFilePost.getUploadId());
			String tempFilePath=appConfig.getProjectFolder()+Constants.FILE_FOLDER+Constants.FILE_FOLDER_TEMP+fileDto.getFilePath();

			File tempFile=new File(tempFilePath);
			String targetFilePath=appConfig.getProjectFolder()+Constants.FILE_FOLDER+Constants.FILE_VIDEO+fileDto.getFilePath();

			File targetFile=new File(targetFilePath);
			FileUtils.copyDirectory(targetFile,targetFile);

			//删除临时目录
			FileUtils.forceDelete(tempFile);
			redisComponent.delVideoFileInfo(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());

			//合并文件
			String completeVideo=targetFilePath+Constants.TEMP_VIDEO_NAME;
			union(targetFilePath,completeVideo,true);

			//获取播放时长
			Integer duration=fFmpegUtils.getVideoInfoDuration(completeVideo);
			updateFilePost.setDuration(duration);
			updateFilePost.setFileSize(new File(completeVideo).length());
			updateFilePost.setFilePath(Constants.FILE_COVER+fileDto.getFilePath());
			updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());
			this.convertVideo2Ts(completeVideo);

		}catch (Exception e){
			log.error("文件转码失败",e);
			updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
		}finally {
			videoInfoFilePostMapper.updateByUploadIdAndUserId(updateFilePost,videoInfoFilePost.getUploadId(),videoInfoFilePost.getUserId());

			VideoInfoFilePostQuery filePostQuery=new VideoInfoFilePostQuery();
			filePostQuery.setVideoId(videoInfoFilePost.getVideoId());
			filePostQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
			//记录转码失败的数量
			Integer failCount=videoInfoFilePostMapper.selectCount(filePostQuery);
			if (failCount > 0) {
				VideoInfoPost videoUpdate=new VideoInfoPost();
				videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
				videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFilePost.getVideoId());
				return;
			}
			filePostQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
			Integer transferCount=videoInfoFilePostMapper.selectCount(filePostQuery);
			if (transferCount==0){
				Integer duration=videoInfoFilePostMapper.sumDuration(videoInfoFilePost.getVideoId());
				VideoInfoPost videoUpdate=new VideoInfoPost();
				videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
				videoUpdate.setDuration(duration);
				videoInfoPostMapper.updateByVideoId(videoUpdate,videoInfoFilePost.getVideoId());
			}
		}
	}

	//将源文件进行转码
	private void convertVideo2Ts(String completeVideo){
		File videoFile=new File(completeVideo);
		File tsFolder=videoFile.getParentFile();
		String codec=fFmpegUtils.getVideoCodec(completeVideo);
		if (Constants.VIDEO_CODE_HEVC.equals(codec)) {
			String tempFileName=completeVideo+Constants.VIDEO_CODE_TEMP_SUFFIX;
			new File(completeVideo).renameTo(new File(tempFileName));
			fFmpegUtils.convertHevc2Mp4(tempFileName,completeVideo);
			new File(tempFileName).delete();
		}
		fFmpegUtils.convertVideo2Ts(tsFolder,completeVideo);
		videoFile.delete();
	}

	private void union(String dirPath,String toFilePath,Boolean delSource){
		File dir=new File(dirPath);
		if (!dir.exists()){
			throw new BusinessException("目录不存在");
		}
		File fileList[]=dir.listFiles();
		File targetFile=new File(toFilePath);
		try (RandomAccessFile writeFile=new RandomAccessFile(targetFile,"rw")){
			byte[] b=new byte[1024*10];
			for (int i=0;i<fileList.length;i++){
				int len=-1;
				//创建读块文件的对象
				File chunkFile=new File(dirPath+File.separator+i);
				RandomAccessFile readFile=null;
				try {
					readFile=new RandomAccessFile(chunkFile,"r");
					while ((len=readFile.read(b))!=-1){
						writeFile.write(b,0,len);
					}
				}catch (Exception e){
					log.error("合并分片失败",e);
					throw new BusinessException("合并文件失败");
				}finally {
					readFile.close();
				}
			}
		}catch (Exception e){
			throw new BusinessException("合并文件"+dirPath+"出错了");
		}finally {
			if (delSource){
				for (int i=0;i<fileList.length;i++){
					fileList[i].delete();
				}
			}
		}
	}

}