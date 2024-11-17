package com.easylive.admin.controller;


import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.query.CategoryInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.CategoryInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController extends ABaseController{

    @Resource
    private CategoryInfoService categoryInfoService;


    @RequestMapping("/loadCategory")
    public ResponseVO loadCategory(CategoryInfoQuery query){
        query.setOrderBy("sort desc");
        query.setConvert2Tree(true);

        List<CategoryInfo> categoryInfos=this.categoryInfoService.findListByParam(query);
        return getSuccessResponseVO(categoryInfos);
    }

    @RequestMapping("/saveCategory")
    public ResponseVO saveCategory(@NotNull Integer pCategory,
                                   Integer categoryId,
                                   @NotEmpty String categoryCode,
                                   @NotEmpty String categoryName,
                                   String icon,
                                   String background){
        CategoryInfo categoryInfo=new CategoryInfo();
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setpCategoryId(pCategory);
        categoryInfo.setCategoryCode(categoryCode);
        categoryInfo.setCategoryName(categoryName);
        categoryInfo.setIcon(icon);
        categoryInfo.setBackground(background);
        categoryInfoService.saveCategory(categoryInfo);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/deleteCategory")
    public ResponseVO deleteCategory(Integer categoryId){
        categoryInfoService.deleteCategory(categoryId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/changeSort")
    public ResponseVO changeSort(@NotNull Integer pCategoryId,@NotEmpty String categoryIds){
        categoryInfoService.changeSort(pCategoryId,categoryIds);
        return getSuccessResponseVO(null);
    }

}
