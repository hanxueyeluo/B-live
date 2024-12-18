package com.easylive.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.exception.BusinessException;
import org.elasticsearch.common.recycler.Recycler;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.CategoryInfoQuery;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mapper.CategoryInfoMapper;
import com.easylive.service.CategoryInfoService;
import com.easylive.utils.StringTools;


/**
 * 分类信息 业务接口实现
 */
@Service("categoryInfoService")
public class CategoryInfoServiceImpl implements CategoryInfoService {

	@Resource
	private CategoryInfoMapper<CategoryInfo, CategoryInfoQuery> categoryInfoMapper;

	@Resource
	private RedisComponent redisComponent;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<CategoryInfo> findListByParam(CategoryInfoQuery param) {
		List<CategoryInfo> categoryInfoList=this.categoryInfoMapper.selectList(param);
		if (param.getConvert2Tree()!=null&&param.getConvert2Tree()){
			categoryInfoList=convertLine2Tree(categoryInfoList, Constants.ZERO);
		}
		return categoryInfoList;
	}

	private List<CategoryInfo> convertLine2Tree(List<CategoryInfo> dateList,Integer pid){
		List<CategoryInfo> children=new ArrayList<>();
		for (CategoryInfo m: dateList) {
			if (m.getCategoryId()!=null&&m.getpCategoryId()!=null&&m.getpCategoryId().equals(pid)){
				m.setChildren(convertLine2Tree(dateList,m.getpCategoryId()));
				children.add(m);
			}
		}
		return children;
	}


	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(CategoryInfoQuery param) {
		return this.categoryInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<CategoryInfo> findListByPage(CategoryInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<CategoryInfo> list = this.findListByParam(param);
		PaginationResultVO<CategoryInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(CategoryInfo bean) {
		return this.categoryInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.categoryInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.categoryInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(CategoryInfo bean, CategoryInfoQuery param) {
		StringTools.checkParam(param);
		return this.categoryInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(CategoryInfoQuery param) {
		StringTools.checkParam(param);
		return this.categoryInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据CategoryId获取对象
	 */
	@Override
	public CategoryInfo getCategoryInfoByCategoryId(Integer categoryId) {
		return this.categoryInfoMapper.selectByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryId修改
	 */
	@Override
	public Integer updateCategoryInfoByCategoryId(CategoryInfo bean, Integer categoryId) {
		return this.categoryInfoMapper.updateByCategoryId(bean, categoryId);
	}

	/**
	 * 根据CategoryId删除
	 */
	@Override
	public Integer deleteCategoryInfoByCategoryId(Integer categoryId) {
		return this.categoryInfoMapper.deleteByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryCode获取对象
	 */
	@Override
	public CategoryInfo getCategoryInfoByCategoryCode(String categoryCode) {
		return this.categoryInfoMapper.selectByCategoryCode(categoryCode);
	}

	/**
	 * 根据CategoryCode修改
	 */
	@Override
	public Integer updateCategoryInfoByCategoryCode(CategoryInfo bean, String categoryCode) {
		return this.categoryInfoMapper.updateByCategoryCode(bean, categoryCode);
	}

	/**
	 * 根据CategoryCode删除
	 */
	@Override
	public Integer deleteCategoryInfoByCategoryCode(String categoryCode) {
		return this.categoryInfoMapper.deleteByCategoryCode(categoryCode);
	}

	@Override
	public void saveCategory(CategoryInfo categoryInfo) {
		CategoryInfo dbInfo=this.categoryInfoMapper.selectByCategoryCode(categoryInfo.getCategoryCode());
		if (categoryInfo.getCategoryId()==null&&dbInfo!=null||categoryInfo.getCategoryId()!=null&&dbInfo!=null&&!categoryInfo.getCategoryId().equals(dbInfo.getCategoryId())){
			throw new BusinessException("分类编号已存在");
		}
		if (categoryInfo.getCategoryId()==null){
			Integer maxSort=this.categoryInfoMapper.selectMaxSort(categoryInfo.getCategoryId());
			categoryInfo.setSort(maxSort+1);
			this.categoryInfoMapper.insert(categoryInfo);
		}else{
			this.categoryInfoMapper.updateByCategoryId(categoryInfo,categoryInfo.getCategoryId());
		}
		save2redis();
	}

	@Override
	public void deleteCategory(Integer categoryId) {
		CategoryInfoQuery categoryInfoQuery=new CategoryInfoQuery();
		categoryInfoQuery.setCategoryIdOrpCategoryId(categoryId);
		categoryInfoMapper.deleteByParam(categoryInfoQuery);
		save2redis();
	}

	@Override
	public void changeSort(Integer pCategoryId, String categoryIds) {
		String[] categoryIdArray=categoryIds.split(",");
		List<CategoryInfo> categoryInfoList=new ArrayList<>();
		Integer sort=0;
		for (String categoryId:categoryIdArray){
			CategoryInfo categoryInfo=new CategoryInfo();
			categoryInfo.setCategoryId(Integer.parseInt(categoryId));
			categoryInfo.setpCategoryId(pCategoryId);
			categoryInfo.setSort(sort);
			categoryInfoList.add(categoryInfo);
		}
		categoryInfoMapper.updateSortBatch(categoryInfoList);
		save2redis();
	}

	private void save2redis(){
		CategoryInfoQuery categoryInfoQuery=new CategoryInfoQuery();
		categoryInfoQuery.setOrderBy("sort asc");
		categoryInfoQuery.setConvert2Tree(true);
		List<CategoryInfo> categoryInfoList=findListByParam(categoryInfoQuery);
		redisComponent.saveCategoryList(categoryInfoList);
	}

	@Override
	public List<CategoryInfo> getAllCategoryList() {
		List<CategoryInfo> categoryInfoList=redisComponent.getCategoryList();
		if (categoryInfoList.isEmpty()) {
			save2redis();
		}
		return redisComponent.getCategoryList();
	}
}