package com.douding.business.controller.admin;




import com.douding.server.dto.*;
import com.douding.server.service.CourseCategoryService;
import com.douding.server.service.CourseService;
import com.douding.server.util.ValidatorUtil;
import com.github.pagehelper.Page;
import io.lettuce.core.GeoArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/admin/course")
public class CourseController {

    private static final Logger LOG = LoggerFactory.getLogger(CourseController.class);
    //给了日志用的
    public  static final String BUSINESS_NAME ="课程";

    @Resource
    private CourseService courseService;

    @Resource
    private CourseCategoryService courseCategoryService;

    @PostMapping("/list-category/{courseId}")
    public ResponseDto listCategory(@PathVariable(value="courseId")String courseId){
        ResponseDto<List<CourseCategoryDto>> responseDto = new ResponseDto<>();
        List<CourseCategoryDto> categoryDtos = courseCategoryService.listByCourse(courseId);
        responseDto.setContent(categoryDtos);
        return responseDto;
    }

    @RequestMapping("/list")
    public ResponseDto list(CoursePageDto pageDto){
        ResponseDto<PageDto> responseDto = new ResponseDto<>();
        courseService.list(pageDto);
        responseDto.setContent(pageDto);
        return responseDto;
    }

    @PostMapping("/save")
    public ResponseDto save(@RequestBody CourseDto courseDto){
        ResponseDto<CourseDto> responseDto = new ResponseDto<>();
        if(!StringUtils.isEmpty(courseDto.getImage()) && !courseDto.getImage().startsWith("http")) {
            courseDto.setImage("http://127.0.0.1:9003/file/f/" + courseDto.getImage());
        }
        courseService.save(courseDto);
        responseDto.setContent(courseDto);
        return responseDto;
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseDto delete(@PathVariable String id){
        ResponseDto<CourseDto> responseDto = new ResponseDto<>();
        courseService.delete(id);
        return responseDto;
    }

    //课程内容相关查找
    @GetMapping("/find-content/{id}")
    public ResponseDto findContent(@PathVariable String id) {
        ResponseDto<CourseContentDto> responseDto = new ResponseDto<>();
        CourseContentDto courseContentDto = courseService.findContent(id);
        responseDto.setContent(courseContentDto);
        return responseDto;
    }

    //课程内容保存
    @PostMapping("/save-content")
    public ResponseDto saveConent(@RequestBody CourseContentDto contentDto) {
        ResponseDto<CourseContentDto> responseDto = new ResponseDto<>();
        courseService.saveContent(contentDto);
        return responseDto;
    }

    //课程排序
    @PostMapping("/sort")
    public ResponseDto sort(@RequestBody SortDto sortDto){
        ResponseDto<SortDto> responseDto = new ResponseDto<>();
        courseService.sort(sortDto);
        return responseDto;
    }

}//end class