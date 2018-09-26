package com.pinyougou.sellergoods.service;

import java.util.List;
import java.util.Map;

import com.pinyougou.pojo.TbBrand;

import entity.PageResult;
//品牌接口
public interface BrandService {
	//查詢所有品牌
	public List<TbBrand> findAll();
	//分頁查詢
	public PageResult findPage(int pageNum,int pageSize);
	//新增
	public void add(TbBrand brand);
	//修改
	public void upDate(TbBrand tbBrand);
	//根据id获取品牌
	public TbBrand findOne(Long id);
	
	public void delete(Long[] ids);
	
	public PageResult findPage(TbBrand brand, int pageNum,int pageSize);
	public List<Map> selectOptionList();

}
