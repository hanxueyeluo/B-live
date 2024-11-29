package com.easylive.web.controller;


import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.VideoStatusEnum;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.VideoStatusCountInfoVO;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@RequestMapping("/userCenter")
@Validated
@Slf4j
public class UserCenterVideoPostController extends ABaseController{

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private VideoInfoService videoInfoService;

    @RequestMapping("/postVideo")
    public ResponseVO postVideo(String videoId,
                                @NotEmpty String videoCover,
                                @NotEmpty @Size(max = 100) String videoName,
                                @NotNull Integer pCategoryId,
                                Integer categoryId,
                                @NotNull Integer postType,
                                @NotEmpty @Size(max = 300) String tags,
                                @Size(max = 2000) String introduction,
                                @Size(max = 3) String interaction,
                                @NotEmpty String uploadFileList){
        TokenUserInfoDto tokenUserInfoDto=getTokenUserInfoDto();
        List<VideoInfoFilePost> filePostList= JsonUtils.convertJsonArray2List(uploadFileList, VideoInfoFilePost.class);

        VideoInfoPost videoInfoPost=new VideoInfoPost();
        videoInfoPost.setVideoId(videoId);
        videoInfoPost.setVideoCover(videoCover);
        videoInfoPost.setVideoName(videoName);
        videoInfoPost.setpCategoryId(pCategoryId);
        videoInfoPost.setCategoryId(categoryId);
        videoInfoPost.setPostType(postType);
        videoInfoPost.setTags(tags);
        videoInfoPost.setIntroduction(introduction);
        videoInfoPost.setInteraction(interaction);
        videoInfoPost.setUserId(tokenUserInfoDto.getUserId());
        videoInfoPostService.saveVideoInfo(videoInfoPost,filePostList);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadVideoPost")
    public ResponseVO loadVideoPost(Integer status,Integer pageNo,String videoNameFuzzy){
        TokenUserInfoDto tokenUserInfoDto=getTokenUserInfoDto();
        VideoInfoPostQuery infoPostQuery=new VideoInfoPostQuery();
        infoPostQuery.setUserId(tokenUserInfoDto.getUserId());
        infoPostQuery.setPageNo(pageNo);
        infoPostQuery.setOrderBy("v.create_time desc");

        if (status!=null){
            if (status==-1){
                infoPostQuery.setExcludeStatusArray(new Integer[] {VideoStatusEnum.STATUS3.getStatus(),VideoStatusEnum.STATUS4.getStatus()});
            }else {
                infoPostQuery.setStatus(status);
            }
        }
        infoPostQuery.setVideoNameFuzzy(videoNameFuzzy);
        infoPostQuery.setQueryCountInfo(true);
        PaginationResultVO resultVO=videoInfoPostService.findListByPage(infoPostQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getVideoCountInfo")
    public ResponseVO getVideoCountInfo(){
        TokenUserInfoDto tokenUserInfoDto=getTokenUserInfoDto();

        VideoInfoPostQuery videoInfoPostQuery=new VideoInfoPostQuery();

        videoInfoPostQuery.setUserId(tokenUserInfoDto.getUserId());

        videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS3.getStatus());
        Integer auditPassCount=videoInfoPostService.findCountByParam(videoInfoPostQuery);

        videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS4.getStatus());
        Integer audiFailCount=videoInfoPostService.findCountByParam(videoInfoPostQuery);

        videoInfoPostQuery.setStatus(null);
        videoInfoPostQuery.setExcludeStatusArray(new Integer[] {VideoStatusEnum.STATUS3.getStatus(),VideoStatusEnum.STATUS4.getStatus()});
        Integer inProgress=videoInfoPostService.findCountByParam(videoInfoPostQuery);

        VideoStatusCountInfoVO countInfoVO=new VideoStatusCountInfoVO();

        countInfoVO.setAuditPassCount(auditPassCount);
        countInfoVO.setAudiFailCount(audiFailCount);
        countInfoVO.setInProgress(inProgress);

        return getSuccessResponseVO(countInfoVO);
    }

}
