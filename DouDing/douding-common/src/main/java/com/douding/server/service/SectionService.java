package com.douding.server.service;

import com.douding.server.domain.CategoryExample;
import com.douding.server.domain.ChapterExample;
import com.douding.server.domain.Section;
import com.douding.server.domain.SectionExample;
import com.douding.server.dto.SectionDto;
import com.douding.server.dto.PageDto;
import com.douding.server.dto.SectionPageDto;
import com.douding.server.enums.SectionChargeEnum;
import com.douding.server.mapper.SectionMapper;
import com.douding.server.util.CopyUtil;
import com.douding.server.util.UuidUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import java.util.List;


import java.util.Date;


@Service
public class SectionService {

    @Resource
    private SectionMapper sectionMapper;

//    @Resource
//    private CourseService courseService;


    /**
     * 列表查询
     */
    public void list(SectionPageDto sectionPageDto) {
        PageHelper.startPage(sectionPageDto.getPage(), sectionPageDto.getSize());
        SectionExample sectionExample = new SectionExample();
        sectionExample.setOrderByClause("sort asc");
        SectionExample.Criteria criteria = sectionExample.createCriteria();
        if(!StringUtils.isEmpty(sectionPageDto.getChapterId())) {
            criteria.andChapterIdEqualTo(sectionPageDto.getChapterId());
        }
        if(!StringUtils.isEmpty(sectionPageDto.getCourseId())) {
            criteria.andCourseIdEqualTo(sectionPageDto.getCourseId());
        }
        List<Section> sectionList = sectionMapper.selectByExample(sectionExample);
        PageInfo<Section> pageInfo = new PageInfo<>(sectionList);
        sectionPageDto.setTotal(pageInfo.getTotal());
        List<SectionDto> sectionDtoList = CopyUtil.copyList(sectionList, SectionDto.class);
        sectionPageDto.setList(sectionDtoList);
    }


    public void save(SectionDto sectionDto) {

        Section section = CopyUtil.copy(sectionDto, Section.class);
        //判断是新增 还是修改
        if (StringUtils.isEmpty(sectionDto.getId())) {
            this.insert(section);
        } else {
            this.update(section);
        }
    }

    //新增数据
    private void insert(Section section) {
        section.setId(UuidUtil.getShortUuid());
        sectionMapper.insert(section);
    }

    //更新数据
    private void update(Section section) {
        sectionMapper.updateByPrimaryKey(section);
    }

    public void delete(String id) {
        sectionMapper.deleteByPrimaryKey(id);
    }

    /**
     * 查询某一课程下的所有节
     */
    public List<SectionDto> listByCourse(String courseId) {

        SectionExample sectionExample = new SectionExample();
        sectionExample.setOrderByClause("sort asc");
        SectionExample.Criteria criteria = sectionExample.createCriteria();
        criteria.andCourseIdEqualTo(courseId);
        List<Section> sectionList = sectionMapper.selectByExample(sectionExample);
        List<SectionDto> sectionDtoList = CopyUtil.copyList(sectionList, SectionDto.class);
        return sectionDtoList;
    }

}//end class
