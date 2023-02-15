package com.douding.server.service;

import com.douding.server.domain.*;
import com.douding.server.dto.*;
import com.douding.server.enums.CourseStatusEnum;
import com.douding.server.mapper.CategoryMapper;
import com.douding.server.mapper.CourseContentMapper;
import com.douding.server.mapper.CourseMapper;
import com.douding.server.mapper.my.MyCourseMapper;
import com.douding.server.util.CopyUtil;
import com.douding.server.util.UuidUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;


import java.util.Date;
import java.util.stream.Collectors;


@Service
public class CourseService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private MyCourseMapper myCourseMapper;

    @Resource
    private CourseCategoryService myCategoryService;

    @Resource
    private CourseContentMapper courseContentMapper;

    @Resource
    private TeacherService teacherService;

    @Resource
    private ChapterService chapterService;

    @Resource
    private SectionService sectionService;



    private static final Logger LOG = LoggerFactory.getLogger(CourseService.class);


    /**
     * 列表查询：关联课程分类表 web接口
     * @param pageDto
     */
    public void list(CoursePageDto pageDto) {
        PageHelper.startPage(pageDto.getPage(), pageDto.getSize());
        CourseExample courseExample = new CourseExample();
        CourseExample.Criteria criteria = courseExample.createCriteria();
        courseExample.setOrderByClause("updated_at desc");
        if(!StringUtils.isEmpty(pageDto.getStatus())) {
            criteria.andStatusEqualTo(pageDto.getStatus());
        }

        List<CourseDto> courseDtos = null;
        if(!StringUtils.isEmpty(pageDto.getCategoryId())) {
            List<CourseCategoryDto> categoryDtos = myCategoryService.listByCategory(pageDto.getCategoryId());
            courseDtos = categoryDtos.stream().filter(item -> item.getCourseId() != null).map(item -> findCourse(item.getCourseId())).collect(Collectors.toList());

        } else {
            List<Course> courseList = courseMapper.selectByExample(courseExample);
            courseDtos = courseList.stream().map((item) -> findCourse(item.getId())).collect(Collectors.toList());
        }

        PageInfo<CourseDto> pageInfo = new PageInfo<>(courseDtos);
        pageDto.setTotal(pageInfo.getTotal());
        pageDto.setList(courseDtos);
    }


    @Transactional
    public void save(CourseDto courseDto) {
        Course course = CopyUtil.copy(courseDto, Course.class);
        courseDto.setUpdatedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        String courseId;
        //判断是新增 还是修改
        if (StringUtils.isEmpty(courseDto.getId())) {
            courseDto.setCreatedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            courseId = this.insert(course);
        } else {
            courseId = this.update(course);
        }
        List<CategoryDto> categorys = courseDto.getCategorys();
        if(categorys != null) {
            for (CategoryDto category : categorys) {
                CourseCategoryDto courseCategoryDto = new CourseCategoryDto();
                courseCategoryDto.setCategoryId(category.getId());
                courseCategoryDto.setCourseId(courseId);
                myCategoryService.save(courseCategoryDto);
            }
        }
    }

    //新增数据
    private String insert(Course course) {

        course.setId(UuidUtil.getShortUuid());
        courseMapper.insert(course);
        return course.getId();
    }

    //更新数据
    private String update(Course course) {
        courseMapper.updateByPrimaryKey(course);
        return course.getId();
    }

    @Transactional
    public void delete(String id) {
        courseMapper.deleteByPrimaryKey(id);
        courseContentMapper.deleteByPrimaryKey(id);

        List<CourseCategoryDto> categoryDtos = myCategoryService.listByCourse(id);
        for (CourseCategoryDto categoryDto : categoryDtos) {
            myCategoryService.delete(categoryDto.getId());
        }

        List<ChapterDto> chapterDtos = chapterService.listByCourse(id);
        for (ChapterDto chapterDto : chapterDtos) {
            chapterService.delete(chapterDto.getId());
        }

        List<SectionDto> sectionDtos = sectionService.listByCourse(id);
        for (SectionDto sectionDto : sectionDtos) {
            sectionService.delete(sectionDto.getId());
        }

        courseContentMapper.deleteByPrimaryKey(id);
    }

    //更新课程时长
    public void updateTime(@Param("courseId")String courseId){
        myCourseMapper.updateTime(courseId);
    }

    //课程内容相关的操作 查找 新增,修改
    public CourseContentDto findContent(String id) {
        CourseContent courseContent = courseContentMapper.selectByPrimaryKey(id);
        return CopyUtil.copy(courseContent, CourseContentDto.class);
    }

    //新增内容 或者修改内容
    public int saveContent(CourseContentDto contentDto) {
        CourseContent courseContent = CopyUtil.copy(contentDto, CourseContent.class);
        CourseContent oldContent = courseContentMapper.selectByPrimaryKey(contentDto.getId());
        if(oldContent == null) {
            return courseContentMapper.insert(courseContent);
        } else {
            return courseContentMapper.updateByPrimaryKeySelective(courseContent);
        }

    }


    public void sort(SortDto sortDto){
        Course course = courseMapper.selectByPrimaryKey(sortDto.getId());
        course.setSort(sortDto.getNewSort());
        courseMapper.updateByPrimaryKey(course);
    }
    /**
     * 查找某一课程，供web模块用，只能查已发布的
     * @param id
     * @return
     */
    public CourseDto findCourse(String id) {
        Course course = courseMapper.selectByPrimaryKey(id);
        CourseDto courseDto = CopyUtil.copy(course, CourseDto.class);

        CourseContent courseContent = courseContentMapper.selectByPrimaryKey(id);
        if(courseContent != null) {
            String content = courseContent.getContent();
            courseDto.setContent(content);
        }


        TeacherDto teacherDto = teacherService.findById(id);
        courseDto.setTeacher(teacherDto);

        List<CourseCategoryDto> courseCategoryDtoList = myCategoryService.listByCourse(id);
        List<ChapterDto> chapterDtos = chapterService.listByCourse(id);
        List<SectionDto> sectionDtos = sectionService.listByCourse(id);
        List<CategoryDto> categoryDtos = courseCategoryDtoList.stream().map((item) -> {
            return CopyUtil.copy(categoryMapper.selectByPrimaryKey(item.getCategoryId()), CategoryDto.class);
        }).collect(Collectors.toList());
        courseDto.setCategorys(categoryDtos);
        courseDto.setChapters(chapterDtos);
        courseDto.setSections(sectionDtos);
        return courseDto;
    }

    /**
     * 新课列表查询，只查询已发布的，按创建日期倒序
     */
    public List<CourseDto> listNew(PageDto pageDto) {
        PageHelper.startPage(pageDto.getPage(), pageDto.getSize());
        CourseExample courseExample = new CourseExample();
        courseExample.setOrderByClause("created_at desc");
        List<Course> courseList = courseMapper.selectByExample(courseExample);
        List<CourseDto> courseDtos = courseList.stream().map((item) -> {
            return findCourse(item.getId());
        }).collect(Collectors.toList());
        PageInfo<CourseDto> pageInfo = new PageInfo<>(courseDtos);
        pageDto.setTotal(pageInfo.getTotal());
        pageDto.setList(courseDtos);
        return courseDtos;
    }



}//end class
