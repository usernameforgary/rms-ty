package com.qilinxx.rms.controller;

import com.qilinxx.rms.domain.model.*;
import com.qilinxx.rms.service.*;
import com.qilinxx.rms.util.DateKit;
import com.qilinxx.rms.util.FileKit;
import com.qilinxx.rms.util.UploadUtil;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ManagerController {
    @Autowired
    UserInfoService userInfoService;
    @Autowired
    UserMajorService userMajorService;
    @Autowired
    MajorService majorService;
    @Autowired
    ProjectService projectService;
    @Autowired
    UserItemService userItemService;
    @Autowired
    DocumentService documentService;
    @Autowired
    ThesisService thesisService;
    @Autowired
    RewardService rewardService;
    @Autowired
    TextbookService textbookService;
    @Autowired
    MeetingService meetingService;
    @Autowired
    LogService logService;
    @Autowired
    NoticeService noticeService;

    /**
     * @return 来到教师成果管理系统页面
     */
    @GetMapping({"main", "1"})
    public String main(HttpSession session, HttpServletRequest request) {
        //以下代码项目完成修改
        //session.setAttribute("uid",213003);
        //以上代码项目完成时修改
        Object o = session.getAttribute("uid");
        if (o == null)
            return "redirect:/login";
        else {
            //logService.insertLog("管理员登录", "admin", userIp(request));
            return "manager/main";
        }
    }

    /**
     * @return 显示top层
     */
    @GetMapping("top")
    public String top(HttpSession session, Model model) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        model.addAttribute("name", user.getName());
        return "manager/top";
    }

    /**
     * @return 显示左侧功能面板
     */
    @GetMapping("left")
    public String left(HttpSession session, Model model) {
        boolean achievementDisplay = false;
        boolean checkDisplay = false;
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        List<UserMajor> userMajorList = userMajorService.findAllUserMajorByUid((String) session.getAttribute("uid"));
        Map<Integer, Integer> majorMap = new HashMap<>();
        if (userMajorList.size() != 0) {
            List<Major> majorList = new ArrayList<>();
            List<Integer> list = new ArrayList<>();
            for (UserMajor um : userMajorList) {
                Major major = majorService.findMajorBymid(um.getMid());
                if (!list.contains(major.getMid())) {
                    list.add(major.getMid());
                    majorList.add(major);
                }
            }
            model.addAttribute(majorList);
            for (Major major : majorList) {
                int i = projectService.countProjectByMidState(major.getMid(), "0") + thesisService.countThesisByMidState(major.getMid(), "0") + rewardService.countRewardByMidState(major.getMid(), "0") + textbookService.countTextbookByMidState(major.getMid(), "0") + meetingService.countMeetingByMidState(major.getMid(), "0");
                majorMap.put(major.getMid(), i);
            }
            model.addAttribute("majorMap", majorMap);
        }
        if (user.getState().equals("3")) {
            achievementDisplay = true;
            if (userMajorList.size() != 0) {
                checkDisplay = true;
            }
        }
        model.addAttribute("achievementDisplay", achievementDisplay);
        model.addAttribute("checkDisplay", checkDisplay);
        model.addAttribute("user", user);
        return "manager/left";
    }

    /**
     * @return 显示主页
     */
    @GetMapping("index")
    public String index(HttpSession session, Model model) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        model.addAttribute("user", user);
        model.addAttribute("dateKit", new DateKit());
        return "manager/index";
    }

    /**
     * 展示教师个人信息
     *
     * @return 跳转个人信息页面
     */
    @GetMapping("info")
    public String info(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");
        UserInfo user = userInfoService.findUserByUid(uid);
        Major major = majorService.findMajorBymid(user.getMid());
        List<Document> documentList = documentService.findDocumentByItemId(uid);
        model.addAttribute("documentList", documentList);
        model.addAttribute("majorName", major.getName());
        model.addAttribute("user", user);
        model.addAttribute("dateKit", new DateKit());
        return "manager/info/info";
    }

    /**
     * 修改教师个人信息
     *
     * @return 来到修改信息页面
     */
    @GetMapping("info-change")
    public String infoChange(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");
        FileKit.selfMap = FileKit.clearOrInitMap(FileKit.selfMap, uid);
        UserInfo user = userInfoService.findUserByUid(uid);
        Major major = majorService.findMajorBymid(user.getMid());
        List<Major> majorList = majorService.findAllMajor();
        List<Document> documentList = documentService.findDocumentByItemId(uid);
        model.addAttribute("documentList", documentList);
        model.addAttribute("majorList", majorList);
        model.addAttribute("majorName", major.getName());
        model.addAttribute("user", user);
        model.addAttribute("dateKit", new DateKit());
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + uid);
        return "manager/info/info-change";
    }

    /**
     * aja修改个人信息
     *
     * @param user 由页面得到的修改信息
     */
    @PostMapping("ajax-info-change")
    @ResponseBody
    public JSONObject ajaxInfoChange(String key, UserInfo user, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        String uid = (String) session.getAttribute("uid");
        List<MultipartFile> selfFileList = FileKit.selfMap.get(key);
        UserInfo dbUser = userInfoService.findUserByUid(uid);
        boolean same = true;
        if ((selfFileList != null && selfFileList.size() != 0) || !user.getName().equals(dbUser.getName()) || !user.getSex().equals(dbUser.getSex()) || !user.getTitle().equals(dbUser.getTitle()) || !user.getBelong().equals(dbUser.getBelong()) || !user.getMid().equals(dbUser.getMid()) || !user.getProfile().equals(dbUser.getProfile())) {
            same = false;
        }
        if (same) {
            json.put("msg", "请勿重复提交！");
            return json;
        }
        user.setUid(dbUser.getUid());
//        user.setState("2");//账号变为待审核状态
        user.setUpdateTime(DateKit.getUnixTimeLong());
        /**
         * 新建附件记录
         */
        if (selfFileList != null && selfFileList.size() != 0) {
            Document document = new Document();
            document.setItemId(uid);
            document.setItemType("self");
            String fileName = "";
            for (MultipartFile file : selfFileList) {
                fileName = file.getOriginalFilename();
                document.setName(fileName);
                document.setType(fileName.substring(fileName.lastIndexOf(".")));
                document.setPath("upload" + File.separator + user.getUid() + File.separator + "self" + File.separator + fileName);
                documentService.createDocument(document);
                //文件拷贝与删除
                File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "self", fileName);
                File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "self" + File.separator, fileName);
                File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "self" + File.separator);
                pathFile.mkdirs();
                FileCopyUtils.copy(tempFile, targetFile);
                FileKit.deleteFile(tempFile);
            }
            FileKit.selfMap.remove(key);
            File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//self");
            FileKit.deleteFile(dirFile);
        }
        userInfoService.modifyUser(user);
        json.put("msg", "信息提交成功！");
        return json;
    }

    /**
     * ajax 提交个人信息附件
     *
     * @param file 个人信息附件
     * @throws IOException
     */
    @PostMapping("ajax-self-file")
    @ResponseBody
    public JSONObject ajaxSelfFile(String key, MultipartFile file, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        System.out.println(key);
        List<MultipartFile> selfFileList = FileKit.selfMap.get(key);
        if (selfFileList == null) selfFileList = FileKit.clearOrInitList(selfFileList);
        //手动去重
        int i = 0;
        if (selfFileList.size() != 0) {
            for (MultipartFile multipartFile : selfFileList) {
                if (multipartFile.getOriginalFilename().equals(file.getOriginalFilename())) {
                    i++;
                }
            }
        }
        if (i == 0) {
            selfFileList.add(file);
            FileKit.selfMap.put(key, selfFileList);

            String path = UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//self";//存储的根路径 临时文件目录
            File dirFile = new File(path);
            dirFile.mkdirs();
            String fileName = file.getOriginalFilename();//原文件名
            File targetFile = new File(path, fileName);
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(targetFile));
        }
        System.out.println(FileKit.selfMap.size());
        return json;
    }

    /**
     * 修改密码页面
     *
     * @return 来到修改密码页面
     */
    @GetMapping("password-change")
    public String passwordChange() {
        return "manager/info/password-change";
    }

    /**
     * ajax修改密码
     * password3 原密码
     * password  新密码
     */
    @PostMapping("ajax-password-change")
    @ResponseBody
    public JSONObject ajaxInfoChange(UserInfo user, String password3, HttpSession session) {
        JSONObject json = new JSONObject();
        UserInfo dbUser = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        if (!dbUser.getPassword().equals(password3)) {
            json.put("msg", "旧密码错误！");
            return json;
        }
        if (user.getPassword().equals(password3)) {
            json.put("msg", "新旧密码不能相同！");
            return json;
        }
        user.setUid(dbUser.getUid());
        //若是账号第一次使用，通过修改密码激活账号
        if (dbUser.getState().equals("0")) {
            user.setState("3");
            user.setUpdateTime(DateKit.getUnixTimeLong());
            userInfoService.modifyUser(user);
            json.put("msg", "密码修改成功,激活账号！");
            return json;
        }
        user.setUpdateTime(DateKit.getUnixTimeLong());
        userInfoService.modifyUser(user);
        json.put("msg", "密码修改成功！");
        return json;
    }

    @RequestMapping("/logout")
    public String execute(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    //项目上传与其附件保存

    /**
     * @return 来到项目上传页面
     */
    @GetMapping("project-upload")
    public String project(HttpSession session, Model model) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //清空或者初始化fileMap
        FileKit.projectMap = FileKit.clearOrInitMap(FileKit.projectMap, user.getUid());
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//project"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + user.getUid());
        return "manager/upload/project-upload";
    }

    /**
     * ajax 提交项目附件
     *
     * @param file 项目附件
     * @throws IOException
     */
    @PostMapping("ajax-project-file")
    @ResponseBody
    public JSONObject ajaxProjectFile(String key, MultipartFile file, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));

        List<MultipartFile> projectFileList = FileKit.projectMap.get(key);
        if (projectFileList == null) projectFileList = FileKit.clearOrInitList(projectFileList);
        //手动去重
        int i = 0;
        if (projectFileList.size() != 0) {
            for (MultipartFile multipartFile : projectFileList) {
                if (multipartFile.getOriginalFilename().equals(file.getOriginalFilename())) {
                    i++;
                }
            }
        }
        if (i == 0) {
            projectFileList.add(file);
            FileKit.projectMap.put(key, projectFileList);

            String path = UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//project";//存储的根路径 临时文件目录
            File dirFile = new File(path);
            dirFile.mkdirs();
            String fileName = file.getOriginalFilename();//原文件名
            File targetFile = new File(path, fileName);
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(targetFile));
        }
        return json;
    }

    /**
     * ajax提交项目，保存附件
     *
     * @param startTimeDate 研究开始时间
     * @param endTimeDate   研究结束时间
     * @param setTimeDate   立项时间
     * @throws IOException
     */
    @PostMapping("ajax-project-form")
    @ResponseBody
    public JSONObject ajaxProjectForm(String key, HttpSession session, Project project, String startTimeDate, String endTimeDate, String setTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //将日期转化为时间戳
        startTimeDate += " 00:00:00";
        endTimeDate += " 00:00:00";
        setTimeDate += " 00:00:00";
        project.setStartTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(startTimeDate)))));
        project.setEndTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(endTimeDate)))));
        project.setSetTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(setTimeDate)))));
        //以下四种种错误
        //根据
//        int projectNum = projectService.countProjectByNameHostFrom(project.getName(), project.getHost(), project.getProjectSource());
        int projectNum = projectService.countProjectByTopicHostSettime(project.getTopic(), project.getHost(), project.getSetTime());
        if (projectNum != 0) {
            json.put("msg", "该项目已被提交！");
            return json;
        }
//        if(projectService.countProjectByTopic(project.getTopic())!=0){
//            json.put("msg", "该项目题目已被提交！");
//            return json;
//        }
        List<MultipartFile> projectFileList = FileKit.projectMap.get(key);
        if (projectFileList == null || projectFileList.size() == 0) {
            json.put("msg", "请先上传附件！");
            return json;
        }
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //姓名去重，并重新排序
        String member = project.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        nameMap.put(project.getHost(), "");//添加主持人
        if (!nameMap.containsKey(user.getName())) {
            json.put("msg", "此项目与本账号用户无关！");
            return json;
        }
        project.setPeople(member);
        /**
         * 新建项目记录
         */

        project.setPid(UUID.randomUUID().toString().replace("-", ""));
        project.setState("0");
        project.setCreateId(user.getUid());
        project.setMid(user.getMid());
        project.setCreateTime(DateKit.getUnixTimeLong());
        projectService.createProject(project);
        /**
         * 新建用户与项目的关系记录
         */
        UserItem userItem = new UserItem();
        userItem.setItemId(project.getPid());
        userItem.setItemType("project");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }
        /**
         * 新建附件记录
         */
        Document document = new Document();
        document.setItemId(project.getPid());
        document.setItemType("project");
        String fileName = "";
        for (MultipartFile file : projectFileList) {
            fileName = file.getOriginalFilename();
            document.setName(fileName);
            document.setType(fileName.substring(fileName.lastIndexOf(".")));
            //document.setPath("upload" + File.separator + user.getUid() + "\\project\\" + project.getCreateTime() + "\\" + fileName);
            document.setPath("upload" + File.separator + user.getUid() + File.separator + "project" + File.separator + project.getCreateTime() + "/" + fileName);
            documentService.createDocument(document);
            //文件拷贝与删除
            File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "project", fileName);
            File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "project" + File.separator + project.getCreateTime(), fileName);
            File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "project" + File.separator + project.getCreateTime());
            pathFile.mkdirs();
            FileCopyUtils.copy(tempFile, targetFile);
            FileKit.deleteFile(tempFile);
        }
        FileKit.projectMap.remove(key, projectFileList);
        File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//project");
        FileKit.deleteFile(dirFile);
        json.put("msg", "提交成功待审核！");
        return json;
    }


    //论文上传与其附件保存

    /**
     * @return 来到论文上传页面
     */
    @GetMapping("thesis-upload")
    public String thesis(Model model, HttpSession session) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //清空或者初始化fileMap
        FileKit.thesisMap = FileKit.clearOrInitMap(FileKit.thesisMap, user.getUid());
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//thesis"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + user.getUid());
        return "manager/upload/thesis-upload";
    }

    /**
     * ajax 提交论文附件
     *
     * @param file 论文附件
     * @throws IOException
     */
    @PostMapping("ajax-thesis-file")
    @ResponseBody
    public JSONObject ajaxThesisFile(String key, MultipartFile file, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        List<MultipartFile> thesisFileList = FileKit.thesisMap.get(key);
        if (thesisFileList == null) thesisFileList = FileKit.clearOrInitList(thesisFileList);
        //手动去重
        int i = 0;
        if (thesisFileList.size() != 0) {
            for (MultipartFile multipartFile : thesisFileList) {
                if (multipartFile.getOriginalFilename().equals(file.getOriginalFilename())) {
                    i++;
                }
            }
        }
        if (i == 0) {
            thesisFileList.add(file);
            FileKit.thesisMap.put(key, thesisFileList);

            String path = UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//thesis";//存储的根路径 临时文件目录
            File dirFile = new File(path);
            dirFile.mkdirs();
            String fileName = file.getOriginalFilename();//原文件名
            File targetFile = new File(path, fileName);
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(targetFile));
        }
        return json;
    }

    /**
     * ajax 提交论文 保存附件
     *
     * @param startPage 起始页码
     * @param endPage   结束页码
     * @throws IOException
     */
    @PostMapping("ajax-thesis-form")
    @ResponseBody
    public JSONObject ajaxThesisForm(String key, Thesis thesis, String publishTimeDate, Integer startPage, Integer endPage, HttpSession session) throws IOException {
        publishTimeDate += " 00:00:00";
        thesis.setPublishTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(publishTimeDate)))));
        JSONObject json = new JSONObject();
        //以下四种错误
        if (thesis.getDossier() == null && thesis.getIssue() == null) {
            json.put("msg", "期和卷至少填一处");
            return json;
        }
        int thesisNum = thesisService.countThesisByHostName(thesis.getHost(), thesis.getName());
        if (thesisNum != 0) {
            json.put("msg", "该项目已被提交！");
            return json;
        }
        List<MultipartFile> thesisFileList = FileKit.thesisMap.get(key);
        if (thesisFileList == null || thesisFileList.size() == 0) {
            json.put("msg", "请先上传附件！");
            return json;
        }
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put(thesis.getHost(), "");//添加主持人
        //姓名去重，并重新排序
        if (thesis.getPeople() != null && !"".equals(thesis.getPeople())) {
            String member = thesis.getPeople().replace("，", ",").replace("、", ",").trim();
            String[] names = member.split(",");
            String people = "";
            for (String name : names) {
                if (!name.equals("")) {
                    nameMap.put(name, "");
                }
            }
            Iterator iterator1 = nameMap.entrySet().iterator();
            while (iterator1.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator1.next();
                people = people + entry.getKey() + ",";
            }

            thesis.setPeople(member);
        }
        if (!nameMap.containsKey(user.getName()) && !nameMap.containsKey(user.getName() + "*")) {
            json.put("msg", "此论文与本账号用户无关！");
            return json;
        }
        /**
         * 新建论文记录
         */
        thesis.setPageNum(startPage + "-" + endPage);
        thesis.setTid(UUID.randomUUID().toString().replace("-", ""));
        thesis.setState("0");
        thesis.setCreateId(user.getUid());
        thesis.setCreateTime(DateKit.getUnixTimeLong());
        thesis.setMid(user.getMid());
        thesisService.createThesis(thesis);
        /**
         * 新建用户与论文的关系记录
         */
        UserItem userItem = new UserItem();
        userItem.setItemId(thesis.getTid());
        userItem.setItemType("thesis");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName(((String) entry.getKey()).replace("*", ""));
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }
        /**
         * 新建附件记录
         */
        Document document = new Document();
        document.setItemId(thesis.getTid());
        document.setItemType("thesis");
        String fileName = "";
        for (MultipartFile file : thesisFileList) {
            fileName = file.getOriginalFilename();
            document.setName(fileName);
            document.setType(fileName.substring(fileName.lastIndexOf(".")));
            document.setPath("upload" + File.separator + user.getUid() + File.separator + "thesis" + File.separator + thesis.getCreateTime() + File.separator + fileName);
            documentService.createDocument(document);
            //文件拷贝与删除
            File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "thesis", fileName);
            File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "thesis" + File.separator + thesis.getCreateTime(), fileName);
            File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "thesis" + File.separator + thesis.getCreateTime());
            pathFile.mkdirs();
            FileCopyUtils.copy(tempFile, targetFile);
            FileKit.deleteFile(tempFile);
        }
        FileKit.thesisMap.remove(key);
        File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "thesis");
        FileKit.deleteFile(dirFile);

        json.put("msg", "提交成功待审核！");
        return json;
    }


    //奖励上传与其附件保存

    /**
     * @return 来到奖励上传页面
     */
    @GetMapping("reward-upload")
    public String reward(Model model, HttpSession session) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //清空或者初始化fileMap
        FileKit.rewardMap = FileKit.clearOrInitMap(FileKit.rewardMap, user.getUid());
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "reward"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + user.getUid());
        return "manager/upload/reward-upload";
    }

    /**
     * ajax 提交奖励附件
     *
     * @param file 奖励附件
     * @throws IOException
     */
    @PostMapping("ajax-reward-file")
    @ResponseBody
    public JSONObject ajaxRewardFile(String key, MultipartFile file, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        List<MultipartFile> rewardFileList = FileKit.rewardMap.get(key);
        if (rewardFileList == null) rewardFileList = FileKit.clearOrInitList(rewardFileList);
        //手动去重
        int i = 0;
        if (rewardFileList.size() != 0) {
            for (MultipartFile multipartFile : rewardFileList) {
                if (multipartFile.getOriginalFilename().equals(file.getOriginalFilename())) {
                    i++;
                }
            }
        }
        if (i == 0) {
            rewardFileList.add(file);
            FileKit.rewardMap.put(key, rewardFileList);

            String path = UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//reward";//存储的根路径 临时文件目录
            File dirFile = new File(path);
            dirFile.mkdirs();
            String fileName = file.getOriginalFilename();//原文件名
            File targetFile = new File(path, fileName);
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(targetFile));
        }
        return json;
    }

    /**
     * ajax提交奖励，保存附件
     *
     * @throws IOException
     */
    @PostMapping("ajax-reward-form")
    @ResponseBody
    public JSONObject ajaxProjectForm(String key, HttpSession session, Reward reward, String getTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //日期转化为时间戳
        getTimeDate += " 00:00:00";
        reward.setGetTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(getTimeDate)))));

        //姓名去重，并重新排序
        String member = reward.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        reward.setPeople(member);

        //以下两种错误
        int rewardNum = rewardService.countRewardByNamePeopleGetTime(reward.getName(), reward.getPeople(), reward.getGetTime());
        if (rewardNum != 0) {
            json.put("msg", "该奖励已被提交！");
            return json;
        }
        List<MultipartFile> rewardFileList = FileKit.rewardMap.get(key);
        if (rewardFileList == null || rewardFileList.size() == 0) {
            json.put("msg", "请先上传附件！");
            return json;
        }
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        if (!nameMap.containsKey(user.getName())) {
            json.put("msg", "此奖励与本账号用户无关！");
            return json;
        }
        /**
         * 新建奖励记录
         */
        //将日期转化为时间戳
        reward.setGetTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(getTimeDate)))));
        reward.setRid(UUID.randomUUID().toString().replace("-", ""));
        reward.setState("0");
        reward.setCreateId(user.getUid());
        reward.setMid(user.getMid());
        reward.setCreateTime(DateKit.getUnixTimeLong());
        rewardService.createReward(reward);
        /**
         * 新建用户与奖励的关系记录
         */
        UserItem userItem = new UserItem();
        userItem.setItemId(reward.getRid());
        userItem.setItemType("reward");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }
        /**
         * 新建附件记录
         */
        Document document = new Document();
        document.setItemId(reward.getRid());
        document.setItemType("reward");
        String fileName = "";
        for (MultipartFile file : rewardFileList) {
            fileName = file.getOriginalFilename();
            document.setName(fileName);
            document.setType(fileName.substring(fileName.lastIndexOf(".")));
            document.setPath("upload" + File.separator + user.getUid() + File.separator + "reward" + File.separator + reward.getCreateTime() + File.separator + fileName);
            documentService.createDocument(document);
            //文件拷贝与删除
            File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "reward", fileName);
            File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "reward" + File.separator + reward.getCreateTime(), fileName);
            File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "reward" + File.separator + reward.getCreateTime());
            pathFile.mkdirs();
            FileCopyUtils.copy(tempFile, targetFile);
            FileKit.deleteFile(tempFile);
        }
        FileKit.rewardMap.remove(key);
        File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "reward");
        FileKit.deleteFile(dirFile);
        json.put("msg", "提交成功待审核！");
        return json;
    }


    //教材的上传与附件保存

    /**
     * @return 来到教材的上传页面
     */
    @GetMapping("textbook-upload")
    public String textbook(Model model, HttpSession session) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //清空或者初始化fileMap
        FileKit.textbookMap = FileKit.clearOrInitMap(FileKit.textbookMap, user.getUid());
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//textbook"));
        List<Category> categories = Arrays.asList(Category.values());
        model.addAttribute("categories", categories);
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + user.getUid());
        return "manager/upload/textbook-upload";
    }

    /**
     * ajax 提交教材的附件
     *
     * @param file 教材附件
     * @throws IOException
     */
    @PostMapping("ajax-textbook-file")
    @ResponseBody
    public JSONObject ajaxTextbookFile(String key, MultipartFile file, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        List<MultipartFile> textbookFileList = FileKit.textbookMap.get(key);
        if (textbookFileList == null) textbookFileList = FileKit.clearOrInitList(textbookFileList);
        //手动去重
        int i = 0;
        if (textbookFileList.size() != 0) {
            for (MultipartFile multipartFile : textbookFileList) {
                if (multipartFile.getOriginalFilename().equals(file.getOriginalFilename())) {
                    i++;
                }
            }
        }
        if (i == 0) {
            textbookFileList.add(file);
            FileKit.textbookMap.put(key, textbookFileList);

            String path = UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//textbook";//存储的根路径 临时文件目录
            File dirFile = new File(path);
            dirFile.mkdirs();
            String fileName = file.getOriginalFilename();//原文件名
            File targetFile = new File(path, fileName);
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(targetFile));
        }
        return json;
    }

    /**
     * ajax提交教材，保存附件
     *
     * @throws IOException
     */
    @PostMapping("ajax-textbook-form")
    @ResponseBody
    public JSONObject ajaxProjectForm(String key, HttpSession session, Textbook textbook, String publishTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //以下三种种错误
        Integer textBookNum = textbookService.countTextBookByISBN(textbook.getIsbn());
        if (textBookNum != 0) {
            json.put("msg", "该教材/专著已被提交！");
            return json;
        }
        List<MultipartFile> textbookFileList = FileKit.textbookMap.get(key);
        if (textbookFileList == null || textbookFileList.size() == 0) {
            json.put("msg", "请先上传附件！");
            return json;
        }
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //姓名去重，并重新排序
        String member = textbook.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        if (!nameMap.containsKey(user.getName())) {
            json.put("msg", "此教材/专著与本账号用户无关！");
            return json;
        }
        textbook.setPeople(member);
        /**
         * 新建项目记录
         */

        //将日期转化为时间戳
        publishTimeDate += "-01 00:00:00";
        Date date = DateKit.dateFormat(publishTimeDate);
        Calendar n = Calendar.getInstance();
        n.setTime(date);
        int month = n.get(Calendar.MONTH);
        n.set(Calendar.MONTH, month);
        textbook.setPublishTime(DateKit.getUnixTimeLong(n.getTime()));
        textbook.setId(UUID.randomUUID().toString().replace("-", ""));
        textbook.setState("0");
        textbook.setCreateId(user.getUid());
        textbook.setMid(user.getMid());
        textbook.setCreateTime(DateKit.getUnixTimeLong());
        textbookService.createTextbook(textbook);
        /**
         * 新建用户与项目的关系记录
         */
        UserItem userItem = new UserItem();
        userItem.setItemId(textbook.getId());
        userItem.setItemType("textbook");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }
        /**
         * 新建附件记录
         */
        Document document = new Document();
        document.setItemId(textbook.getId());
        document.setItemType("textbook");
        String fileName = "";
        for (MultipartFile file : textbookFileList) {
            fileName = file.getOriginalFilename();
            document.setName(fileName);
            document.setType(fileName.substring(fileName.lastIndexOf(".")));
            document.setPath("upload" + File.separator + user.getUid() + File.separator + "textbook" + File.separator + textbook.getCreateTime() + File.separator + fileName);
            documentService.createDocument(document);
            //文件拷贝与删除
            File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//textbook", fileName);
            File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "textbook" + File.separator + textbook.getCreateTime(), fileName);
            File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "textbook" + File.separator + textbook.getCreateTime());
            pathFile.mkdirs();
            FileCopyUtils.copy(tempFile, targetFile);
            FileKit.deleteFile(tempFile);
        }
        FileKit.textbookMap.remove(key);
        File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//textbook");
        FileKit.deleteFile(dirFile);

        json.put("msg", "提交成功待审核！");
        return json;
    }


    //会议的上传与附件保存

    /**
     * @return 来到会议上传页面
     */
    @GetMapping("meeting-upload")
    public String meeting(HttpSession session, Model model) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //清空或者初始化fileMap
        FileKit.meetingMap = FileKit.clearOrInitMap(FileKit.meetingMap, user.getUid());
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//meeting"));
        //添加key值
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + user.getUid());
        return "manager/upload/meeting-upload";
    }

    /**
     * ajax 提交会议的附件
     *
     * @param file 会议附件
     * @throws IOException
     */
    @PostMapping("ajax-meeting-file")
    @ResponseBody
    public JSONObject ajaxMeetingFile(MultipartFile file, HttpSession session, String key) throws IOException {
        JSONObject json = new JSONObject();
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));

        List<MultipartFile> meetingFileList = FileKit.meetingMap.get(key);
        if (meetingFileList == null) meetingFileList = FileKit.clearOrInitList(meetingFileList);
        //手动去重
        int i = 0;
        if (meetingFileList.size() != 0) {
            for (MultipartFile multipartFile : meetingFileList) {
                if (multipartFile.getOriginalFilename().equals(file.getOriginalFilename())) {
                    i++;
                }
            }
        }
        if (i == 0) {
            meetingFileList.add(file);
            FileKit.meetingMap.put(key, meetingFileList);

            String path = UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//meeting";//存储的根路径 临时文件目录
            File dirFile = new File(path);
            dirFile.mkdirs();
            String fileName = file.getOriginalFilename();//原文件名
            File targetFile = new File(path, fileName);
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(targetFile));
        }
        return json;
    }

    /**
     * ajax提交会议，保存附件
     *
     * @throws IOException
     */
    @PostMapping("ajax-meeting-form")
    @ResponseBody
    public JSONObject ajaxMeetingForm(String key, HttpSession session, Meeting meeting, String startTimeDate, String endTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //以下三种种错误
        startTimeDate += ":00";
        endTimeDate += ":00";
        meeting.setStartTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(startTimeDate)))));
        meeting.setEndTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(endTimeDate)))));
        Integer meetingNum = meetingService.countMeetingByNameMeetingTime(meeting.getName(), meeting.getStartTime());
        if (meetingNum != 0) {
            json.put("msg", "该会议已被提交！");
            return json;
        }

        List<MultipartFile> meetingFileList = FileKit.meetingMap.get(key);
        if (meetingFileList == null || meetingFileList.size() == 0) {
            json.put("msg", "请先上传附件！");
            return json;
        }
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        //姓名去重，并重新排序
        String member = meeting.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        if (!nameMap.containsKey(user.getName())) {
            json.put("msg", "此会议与本账号用户无关！");
            return json;
        }
        meeting.setPeople(member);
        /**
         * 新建项目记录
         */

        //保存会议信息
        meeting.setId(UUID.randomUUID().toString().replace("-", ""));
        meeting.setState("0");
        meeting.setCreateId(user.getUid());
        meeting.setMid(user.getMid());
        meeting.setCreateTime(DateKit.getUnixTimeLong());
        meetingService.createMeeting(meeting);
        /**
         * 新建用户与项目的关系记录
         */
        UserItem userItem = new UserItem();
        userItem.setItemId(meeting.getId());
        userItem.setItemType("meeting");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }
        /**
         * 新建附件记录
         */
        Document document = new Document();
        document.setItemId(meeting.getId());
        document.setItemType("meeting");
        String fileName = "";
        for (MultipartFile file : meetingFileList) {
            fileName = file.getOriginalFilename();
            document.setName(fileName);
            document.setType(fileName.substring(fileName.lastIndexOf(".")));
            document.setPath("upload" + File.separator + user.getUid() + File.separator + "meeting" + File.separator + meeting.getCreateTime() + File.separator + fileName);
            documentService.createDocument(document);
            //文件拷贝与删除
            File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "meeting", fileName);
            File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "meeting" + File.separator + meeting.getCreateTime(), fileName);
            File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "meeting" + File.separator + meeting.getCreateTime());
            pathFile.mkdirs();
            FileCopyUtils.copy(tempFile, targetFile);
            FileKit.deleteFile(tempFile);
        }
        FileKit.meetingMap.remove(key);
        File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "meeting");
        FileKit.deleteFile(dirFile);
        json.put("msg", "提交成功待审核！");
        return json;
    }

    @RequestMapping("showNotice")
    public String showNotice(String type, HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");
        List<Notice> notices = noticeService.getAllNoticesOrderByTime(uid, type);
        model.addAttribute("notices", notices);
        model.addAttribute("dateKit", new DateKit());
        return "manager/notice";
    }

    //项目、论文、奖励、教材、会议的总览页面

    /**
     * @return 来到项目总览页面
     */
    @GetMapping("project-overview")
    public String projectOverview(HttpSession session, Model model) {

        String uid = (String) session.getAttribute("uid");
        //获取用户是否具有查看权限
        List<Integer> midList = userMajorService.findMidListByUidAndPower(uid, 2);

        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);

        List<String> uids = new ArrayList<>();
        //查看该mid下用户id
        midList.forEach(e -> {
            uids.addAll(userInfoService.findUserByMid(e).stream().map(u -> u.getUid()).collect(Collectors.toList()));
        });
        uids.add(uid);

        List<UserItem> userItemList = userItemService.findUserItemByUidListItemType(uids, "project");
        List<Project> projectList = new ArrayList<>();
        Map<String, UserInfo> createrMap = new HashMap<>();
        for (UserItem userItem : userItemList) {
            Project project = projectService.findProjectByPid(userItem.getItemId());
            Notice notice = noticeService.selectLastNotice(project.getPid(), "project");
            if (notice != null) project.setRemake(notice.getMessage());
            project.setIsAudit(midList1.contains(project.getMid()));
            projectList.add(project);
        }
        for (Project project : projectList) {
            createrMap.put(project.getCreateId(), userInfoService.findUserByUid(project.getCreateId()));
        }
        //将projectList倒序
        Collections.reverse(projectList);
        Notice first = noticeService.latestNotice(uid, "project");
        model.addAttribute("first", first);
        model.addAttribute("uid", uid);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("projectList", projectList);
        model.addAttribute("dateKit", new DateKit());
        return "manager/overview/project-overview";
    }

    /**
     * @return 来到论文总览页面
     */
    @GetMapping("thesis-overview")
    public String thesisOverview(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");

        //获取用户是否具有查看权限
        List<Integer> midList = userMajorService.findMidListByUidAndPower(uid, 2);
//获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);

        List<String> uids = new ArrayList<>();
        //查看该mid下用户id
        midList.forEach(e -> {
            uids.addAll(userInfoService.findUserByMid(e).stream().map(u -> u.getUid()).collect(Collectors.toList()));
        });
        uids.add(uid);
        List<UserItem> userItemList = userItemService.findUserItemByUidListItemType(uids, "thesis");

        List<Thesis> thesisList = new ArrayList<>();
        Map<String, UserInfo> createrMap = new HashMap<>();
        for (UserItem userItem : userItemList) {
            Thesis thesis = thesisService.findThesisByTid(userItem.getItemId());
            Notice notice = noticeService.selectLastNotice(thesis.getTid(), "thesis");
            thesis.setIsAudit(midList1.contains(thesis.getMid()));
            if (notice != null) thesis.setRemake(notice.getMessage());
            thesisList.add(thesis);
        }
        for (Thesis thesis : thesisList) {
            createrMap.put(thesis.getCreateId(), userInfoService.findUserByUid(thesis.getCreateId()));
        }
        //将projectList倒序
        Collections.reverse(thesisList);
        Notice first = noticeService.latestNotice(uid, "thesis");
        model.addAttribute("first", first);
        model.addAttribute("uid", uid);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("thesisList", thesisList);
        model.addAttribute("dateKit", new DateKit());
        return "manager/overview/thesis-overview";
    }

    /**
     * @return 来到奖励总览页面
     */
    @GetMapping("reward-overview")
    public String rewardOverview(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");

        //获取用户是否具有查看权限
        List<Integer> midList = userMajorService.findMidListByUidAndPower(uid, 2);
//获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);

        List<String> uids = new ArrayList<>();
        //查看该mid下用户id
        midList.forEach(e -> {
            uids.addAll(userInfoService.findUserByMid(e).stream().map(u -> u.getUid()).collect(Collectors.toList()));
        });
        uids.add(uid);
        List<UserItem> userItemList = userItemService.findUserItemByUidListItemType(uids, "reward");
        List<Reward> rewardList = new ArrayList<>();
        Map<String, UserInfo> createrMap = new HashMap<>();
        for (UserItem userItem : userItemList) {
            Reward reward = rewardService.findRewardByRid(userItem.getItemId());
            reward.setIsAudit(midList1.contains(reward.getMid()));
            Notice notice = noticeService.selectLastNotice(reward.getRid(), "reward");
            if (notice != null) reward.setRemake(notice.getMessage());
            rewardList.add(reward);
        }
        for (Reward reward : rewardList) {
            createrMap.put(reward.getCreateId(), userInfoService.findUserByUid(reward.getCreateId()));
        }
        //将rewardList倒序
        Collections.reverse(rewardList);
        Notice first = noticeService.latestNotice(uid, "reward");
        model.addAttribute("first", first);
        model.addAttribute("uid", uid);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("rewardList", rewardList);
        model.addAttribute("dateKit", new DateKit());
        return "manager/overview/reward-overview";
    }

    /**
     * @return 来到教材总览页面
     */
    @GetMapping("textbook-overview")
    public String textbookOverview(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");


        //获取用户是否具有查看权限
        List<Integer> midList = userMajorService.findMidListByUidAndPower(uid, 2);
        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);

        List<String> uids = new ArrayList<>();
        //查看该mid下用户id
        midList.forEach(e -> {
            uids.addAll(userInfoService.findUserByMid(e).stream().map(u -> u.getUid()).collect(Collectors.toList()));
        });
        uids.add(uid);

        List<UserItem> userItemList = userItemService.findUserItemByUidListItemType(uids, "textbook");
        List<Textbook> textbookList = new ArrayList<>();
        Map<String, UserInfo> createrMap = new HashMap<>();
        for (UserItem userItem : userItemList) {
            Textbook textbook = textbookService.findTextbookById(userItem.getItemId());
            textbook.setIsAudit(midList1.contains(textbook.getMid()));
            Notice notice = noticeService.selectLastNotice(textbook.getId(), "textbook");
            if (notice != null) textbook.setRemake(notice.getMessage());
            textbookList.add(textbook);
        }
        for (Textbook textbook : textbookList) {
            createrMap.put(textbook.getCreateId(), userInfoService.findUserByUid(textbook.getCreateId()));
        }
        //将projectList倒序
        Collections.reverse(textbookList);
        Notice first = noticeService.latestNotice(uid, "textbook");
        model.addAttribute("first", first);
        model.addAttribute("uid", uid);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("textbookList", textbookList);
        model.addAttribute("dateKit", new DateKit());
        return "manager/overview/textbook-overview";
    }

    /**
     * @return 来到会议总览页面
     */
    @GetMapping("meeting-overview")
    public String meetingOverview(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");

        //获取用户是否具有查看权限
        List<Integer> midList = userMajorService.findMidListByUidAndPower(uid, 2);
//获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);

        List<String> uids = new ArrayList<>();
        //查看该mid下用户id
        midList.forEach(e -> {
            uids.addAll(userInfoService.findUserByMid(e).stream().map(u -> u.getUid()).collect(Collectors.toList()));
        });
        uids.add(uid);
        List<UserItem> userItemList = userItemService.findUserItemByUidListItemType(uids, "meeting");
        List<Meeting> meetingList = new ArrayList<>();
        Map<String, UserInfo> createrMap = new HashMap<>();
        for (UserItem userItem : userItemList) {
            Meeting meeting = meetingService.findMeetingById(userItem.getItemId());
            meeting.setIsAudit(midList1.contains(meeting.getMid()));
            Notice notice = noticeService.selectLastNotice(meeting.getId(), "meeting");
            if (notice != null) meeting.setRemake(notice.getMessage());
            meetingList.add(meeting);
        }
        for (Meeting meeting : meetingList) {
            createrMap.put(meeting.getCreateId(), userInfoService.findUserByUid(meeting.getCreateId()));
        }
        //将projectList倒序
        Collections.reverse(meetingList);
        Notice first = noticeService.latestNotice(uid, "meeting");
        model.addAttribute("first", first);
        model.addAttribute("uid", uid);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("meetingList", meetingList);
        model.addAttribute("dateKit", new DateKit());
        return "manager/overview/meeting-overview";
    }


    //item公共使用方法

    /**
     * ajax删除所传id  的item
     *
     * @param itemType 为item类别
     * @param id       为itemId
     */
    @PostMapping("ajax-item-delete")
    @ResponseBody
    public JSONObject ajaxItemDelete(String itemType, String id) {
        JSONObject json = new JSONObject();
        switch (itemType) {
            case "project":
                projectService.deleteProjectByPid(id);
                userItemService.deleteUserItemByItemId(id);
                List<Document> documentList1 = documentService.findDocumentByItemId(id);
                if (documentList1.size() != 0) {
                    FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + documentList1.get(0).getPath()));
                    //FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + "//" + documentList1.get(0).getPath()).getParentFile());
                }
                documentService.deleteDocumentByItemId(id);
                break;
            case "thesis":
                thesisService.deleteThesisByTid(id);
                userItemService.deleteUserItemByItemId(id);
                List<Document> documentList2 = documentService.findDocumentByItemId(id);
                if (documentList2.size() != 0) {
                    FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + documentList2.get(0).getPath()));
                    //FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + "//" + documentList2.get(0).getPath()).getParentFile());
                }
                documentService.deleteDocumentByItemId(id);
                break;
            case "reward":
                rewardService.deleteReward(id);
                userItemService.deleteUserItemByItemId(id);
                List<Document> documentList3 = documentService.findDocumentByItemId(id);
                if (documentList3.size() != 0) {
                    FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + documentList3.get(0).getPath()));
                    //FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + "//" + documentList3.get(0).getPath()).getParentFile());
                }
                documentService.deleteDocumentByItemId(id);
                break;
            case "textbook":
                textbookService.deleteTextbookById(id);
                userItemService.deleteUserItemByItemId(id);
                List<Document> documentList4 = documentService.findDocumentByItemId(id);
                if (documentList4.size() != 0) {
                    //FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + "//" + documentList4.get(0).getPath()).getParentFile());
                    FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + documentList4.get(0).getPath()));
                }
                documentService.deleteDocumentByItemId(id);
                break;
            case "meeting":
                meetingService.deleteMeetingByid(id);
                userItemService.deleteUserItemByItemId(id);
                List<Document> documentList5 = documentService.findDocumentByItemId(id);
                if (documentList5.size() != 0) {
                    //FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + "//" + documentList5.get(0).getPath()).getParentFile());
                    FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + documentList5.get(0).getPath()));
                }
                documentService.deleteDocumentByItemId(id);
                break;
            default:
                json.put("msg", "删除失败！");
                return json;
        }
        json.put("msg", "删除成功！");
        return json;
    }

    /**
     * @param id       itemId
     * @param itemType item的类别
     * @return 来到item的详情页面，以项目详情为主要页面
     */
    @GetMapping("item-detail")
    public String itemDetail(String id, String itemType, Model model, HttpSession session, String from) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        boolean display = true;
        if (!from.equals("user")) {
            display = false;
        }
        Map<String, UserInfo> createrMap = new HashMap<>();
        switch (itemType) {
            case "project":
                Project project = projectService.findProjectByPid(id);
                if (project.getState().equals("2") || !project.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(project.getCreateId(), userInfoService.findUserByUid(project.getCreateId()));
                model.addAttribute("project", project);
                break;
            case "thesis":
                Thesis thesis = thesisService.findThesisByTid(id);
                if (thesis.getState().equals("2") || !thesis.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(thesis.getCreateId(), userInfoService.findUserByUid(thesis.getCreateId()));
                model.addAttribute("thesis", thesis);
                break;
            case "reward":
                Reward reward = rewardService.findRewardByRid(id);
                if (reward.getState().equals("2") || !reward.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(reward.getCreateId(), userInfoService.findUserByUid(reward.getCreateId()));
                model.addAttribute("reward", reward);
                break;
            case "textbook":
                Textbook textbook = textbookService.findTextbookById(id);
                if (textbook.getState().equals("2") || !textbook.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(textbook.getCreateId(), userInfoService.findUserByUid(textbook.getCreateId()));
                model.addAttribute("textbook", textbook);
                break;
            case "meeting":
                Meeting meeting = meetingService.findMeetingById(id);
                if (meeting.getState().equals("2") || !meeting.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(meeting.getCreateId(), userInfoService.findUserByUid(meeting.getCreateId()));
                model.addAttribute("meeting", meeting);
                break;
        }

        List<Document> documentList = documentService.findDocumentByItemId(id);
        model.addAttribute("display", display);
        model.addAttribute("documentList", documentList);
        model.addAttribute("itemType", itemType);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("dateKit", new DateKit());
        return "manager/detail/item-detail";
    }


    /**
     * @param id       itemId
     * @param itemType item的类别
     * @return 来到item的详情页面, 以附件面为主要页面
     */
    @GetMapping("item-detail-file")
    public String itemDetailFile(String id, String itemType, Model model, HttpSession session, String from) {
        UserInfo user = userInfoService.findUserByUid((String) session.getAttribute("uid"));
        boolean display = true;
        if (!from.equals("user")) {
            display = false;
        }
        Map<String, UserInfo> createrMap = new HashMap<>();
        switch (itemType) {
            case "project":
                Project project = projectService.findProjectByPid(id);
                if (project.getState().equals("2") || !project.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(project.getCreateId(), userInfoService.findUserByUid(project.getCreateId()));
                model.addAttribute("project", project);
                break;
            case "thesis":
                Thesis thesis = thesisService.findThesisByTid(id);
                if (thesis.getState().equals("2") || !thesis.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(thesis.getCreateId(), userInfoService.findUserByUid(thesis.getCreateId()));
                model.addAttribute("thesis", thesis);
                break;
            case "reward":
                Reward reward = rewardService.findRewardByRid(id);
                if (reward.getState().equals("2") || !reward.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(reward.getCreateId(), userInfoService.findUserByUid(reward.getCreateId()));
                model.addAttribute("reward", reward);
                break;
            case "textbook":
                Textbook textbook = textbookService.findTextbookById(id);
                if (textbook.getState().equals("2") || !textbook.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(textbook.getCreateId(), userInfoService.findUserByUid(textbook.getCreateId()));
                model.addAttribute("textbook", textbook);
                break;
            case "meeting":
                Meeting meeting = meetingService.findMeetingById(id);
                if (meeting.getState().equals("2") || !meeting.getCreateId().equals(user.getUid())) {
                    display = false;
                }
                createrMap.put(meeting.getCreateId(), userInfoService.findUserByUid(meeting.getCreateId()));
                model.addAttribute("meeting", meeting);
                break;
        }

        List<Document> documentList = documentService.findDocumentByItemId(id);
        model.addAttribute("display", display);
        model.addAttribute("documentList", documentList);
        model.addAttribute("itemType", itemType);
        model.addAttribute("createrMap", createrMap);
        model.addAttribute("dateKit", new DateKit());
        return "manager/detail/item-detail-file";
    }

    /**
     * 附件下载
     *
     * @param did      文件document的id
     * @param response
     * @throws IOException
     */
    @GetMapping("download")
    public void download(Integer did, HttpServletResponse response) throws IOException {
        Document document = documentService.findDocumentByDid(did);
        String fileName = document.getName();// 文件名
        if (fileName != null) {
            //设置文件路径
            File file = new File(UploadUtil.getUploadFilePath() + File.separator + document.getPath());
            //File file = new File(realPath , fileName);
            if (file.exists()) {
                response.setContentType("application/force-download");// 设置强制下载不打开
                response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * ajax 附件删除
     *
     * @param did 文件document的id
     * @return
     */
    @PostMapping("ajax-document-delete")
    @ResponseBody
    public JSONObject ajaxDocumentDelete(Integer did, String itemType) {
        JSONObject json = new JSONObject();

        Document document = documentService.findDocumentByDid(did);
//        List<Document> documentList = documentService.findDocumentByItemId(document.getItemId());
//        if (documentList.size() == 1) {
//            json.put("msg", "至少保留一个文件！");
//            return json;
//        }
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + document.getPath()));
        documentService.deleteDocumentByDid(did);
        //item 记录更新
        switch (itemType) {
            case "project":
                Project project = new Project();
                project.setPid(document.getItemId());
                project.setState("0");
                project.setUpdateTime(DateKit.getUnixTimeLong());
                projectService.updateProject(project);
                break;
            case "thesis":
                Thesis thesis = new Thesis();
                thesis.setTid(document.getItemId());
                thesis.setState("0");
                thesis.setUpdateTime(DateKit.getUnixTimeLong());
                thesisService.updateThesis(thesis);
                break;
            case "reward":
                Reward reward = new Reward();
                reward.setRid(document.getItemId());
                reward.setState("0");
                reward.setUpdateTime(DateKit.getUnixTimeLong());
                rewardService.updateReward(reward);
            case "textbook":
                Textbook textbook = new Textbook();
                textbook.setId(document.getItemId());
                textbook.setState("0");
                textbook.setUpdateTime(DateKit.getUnixTimeLong());
                textbookService.updateTextbook(textbook);
                break;
            case "meeting":
                Meeting meeting = new Meeting();
                meeting.setId(document.getItemId());
                meeting.setState("0");
                meeting.setUpdateTime(DateKit.getUnixTimeLong());
                meetingService.updateMeeting(meeting);
                break;
        }
        json.put("msg", "删除成功！");
        return json;
    }

    /**
     * @param mid 项目的 专业分类
     * @return 来到项目、论文、奖励审核页面
     */
    @GetMapping("check")
    public String check(HttpSession session, Integer mid, Model model) {
        String uid = (String) session.getAttribute("uid");
        Map<String, UserInfo> createrMap = new HashMap<>();
        List<Project> projectList = projectService.findProjectByMid(mid);
        int projectNum = 0, thesisNum = 0, rewardNum = 0, textbookNum = 0, meetingNum = 0;
        boolean check = false;
        boolean see = false;
        if (projectList.size() != 0) {
            for (Project project : projectList) {
                createrMap.put(project.getCreateId(), userInfoService.findUserByUid(project.getCreateId()));
                if (project.getState().equals("0")) {
                    projectNum++;
                }
            }
        }
        List<Thesis> thesisList = thesisService.findThesisByMid(mid);
        if (thesisList.size() != 0) {
            for (Thesis thesis : thesisList) {
                createrMap.put(thesis.getCreateId(), userInfoService.findUserByUid(thesis.getCreateId()));
                if (thesis.getState().equals("0")) {
                    thesisNum++;
                }
            }
        }
        List<Reward> rewardList = rewardService.findRewardByMid(mid);
        if (rewardList.size() != 0) {
            for (Reward reward : rewardList) {
                createrMap.put(reward.getCreateId(), userInfoService.findUserByUid(reward.getCreateId()));
                if (reward.getState().equals("0")) {
                    rewardNum++;
                }
            }
        }
        List<Textbook> textbookList = textbookService.findTextbookByMid(mid);
        if (textbookList.size() != 0) {
            for (Textbook textbook : textbookList) {
                createrMap.put(textbook.getCreateId(), userInfoService.findUserByUid(textbook.getCreateId()));
                if (textbook.getState().equals("0")) {
                    textbookNum++;
                }
            }
        }
        List<Meeting> meetingList = meetingService.findMeetingByMid(mid);
        if (meetingList.size() != 0) {
            for (Meeting meeting : meetingList) {
                createrMap.put(meeting.getCreateId(), userInfoService.findUserByUid(meeting.getCreateId()));
                if (meeting.getState().equals("0")) {
                    meetingNum++;
                }
            }
        }
        List<UserMajor> userMajorList = userMajorService.findAllUserMajorByUidAndMid(uid, mid);
        String power = "";
        for (UserMajor um : userMajorList) {
            power += um.getPower();
        }
        if (power.contains("1")) {
            check = true;
            see = true;
        }
        if (power.contains("2")) {
            see = true;
        }
        model.addAttribute("check", check);
        model.addAttribute("see", see);

        model.addAttribute("projectNum", projectNum);
        model.addAttribute("thesisNum", thesisNum);
        model.addAttribute("rewardNum", rewardNum);
        model.addAttribute("textbookNum", textbookNum);
        model.addAttribute("meetingNum", meetingNum);

        model.addAttribute("thesisList", thesisList);
        model.addAttribute("rewardList", rewardList);
        model.addAttribute("projectList", projectList);
        model.addAttribute("textbookList", textbookList);
        model.addAttribute("meetingList", meetingList);

        model.addAttribute("createrMap", createrMap);
        model.addAttribute("dateKit", new DateKit());
        return "manager/check/check";
    }

    /**
     * 审核通过
     *
     * @param itemType item类别
     * @param id       itemId
     */
    @PostMapping("ajax-item-start")
    @ResponseBody
    public JSONObject ajaxItemStart(String itemType, String id) {
        Integer i = noticeService.passNoticeByNid(id, itemType);
        JSONObject json = new JSONObject();
        switch (itemType) {
            case "project":
                Project project = new Project();
                project.setPid(id);
                project.setState("2");
                project.setUpdateTime(DateKit.getUnixTimeLong());
                projectService.updateProject(project);
                break;
            case "thesis":
                Thesis thesis = new Thesis();
                thesis.setTid(id);
                thesis.setState("2");
                thesis.setUpdateTime(DateKit.getUnixTimeLong());
                thesisService.updateThesis(thesis);
                break;
            case "reward":
                Reward reward = new Reward();
                reward.setRid(id);
                reward.setState("2");
                reward.setUpdateTime(DateKit.getUnixTimeLong());
                rewardService.updateReward(reward);
                break;
            case "textbook":
                Textbook textbook = new Textbook();
                textbook.setId(id);
                textbook.setState("2");
                textbook.setUpdateTime(DateKit.getUnixTimeLong());
                textbookService.updateTextbook(textbook);
                break;
            case "meeting":
                Meeting meeting = new Meeting();
                meeting.setId(id);
                meeting.setState("2");
                meeting.setUpdateTime(DateKit.getUnixTimeLong());
                meetingService.updateMeeting(meeting);
                break;
        }
        json.put("msg", "项目通过审核");
        return json;
    }

    /**
     * 审核不通过
     *
     * @param itemType item类别
     * @param id       itemId
     */
    @Transactional
    @PostMapping("ajax-item-stop")
    @ResponseBody
    public JSONObject ajaxItemStop(HttpSession session, String itemType, String id, String msg) {
        String createId = null;
        JSONObject json = new JSONObject();
        switch (itemType) {
            case "project":
                Project project = new Project();
                project.setPid(id);
                project.setState("1");
                project.setUpdateTime(DateKit.getUnixTimeLong());
                projectService.updateProject(project);
                createId = projectService.findProjectByPid(id).getCreateId();
                break;
            case "thesis":
                Thesis thesis = new Thesis();
                thesis.setTid(id);
                thesis.setState("1");
                thesis.setUpdateTime(DateKit.getUnixTimeLong());
                thesisService.updateThesis(thesis);
                createId = thesisService.findThesisByTid(id).getCreateId();
                break;
            case "reward":
                Reward reward = new Reward();
                reward.setRid(id);
                reward.setState("1");
                reward.setUpdateTime(DateKit.getUnixTimeLong());
                rewardService.updateReward(reward);
                createId = rewardService.findRewardByRid(id).getCreateId();
                break;
            case "textbook":
                Textbook textbook = new Textbook();
                textbook.setId(id);
                textbook.setState("1");
                textbook.setUpdateTime(DateKit.getUnixTimeLong());
                textbookService.updateTextbook(textbook);
                createId = textbookService.findTextbookById(id).getCreateId();
                break;
            case "meeting":
                Meeting meeting = new Meeting();
                meeting.setId(id);
                meeting.setState("1");
                meeting.setUpdateTime(DateKit.getUnixTimeLong());
                meetingService.updateMeeting(meeting);
                createId = meetingService.findMeetingById(id).getCreateId();
                break;
        }
        Notice notice = new Notice();
        notice.setNid(com.qilinxx.rms.util.UUID.UU32());
        notice.setCreateTime(DateKit.getUnixTimeLong());
        notice.setResultId(id);
        notice.setType(itemType);
        notice.setMessage(msg);
        notice.setCheckId((String) session.getAttribute("uid"));
        notice.setCreateId(createId);
        notice.setState("0");
        System.out.println(notice);
        noticeService.addNotice(notice);
        json.put("msg", "未通过审核");
        return json;
    }


    //item重新编辑页面

    /**
     * @param id
     * @return 项目内容编辑页面
     */
    @GetMapping("project-edit")
    public String projectEdit(String id, Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");
        Project project = projectService.findProjectByPid(id);
        List<Document> documentList = documentService.findDocumentByItemId(id);
        FileKit.projectMap = FileKit.clearOrInitMap(FileKit.projectMap, uid);
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + uid + File.separator + "temp//project"));
        model.addAttribute("documentList", documentList);
        model.addAttribute("project", project);
        model.addAttribute("startTime", DateKit.formatDateByUnixTime(project.getStartTime(), "yyyy-MM-dd"));
        model.addAttribute("endTime", DateKit.formatDateByUnixTime(project.getEndTime(), "yyyy-MM-dd"));
        model.addAttribute("setTime", DateKit.formatDateByUnixTime(project.getSetTime(), "yyyy-MM-dd"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + uid);
        return "manager/edit/project-edit";
    }

    /**
     * @param id
     * @return 论文内容编辑页面
     */
    @GetMapping("thesis-edit")
    public String thesisEdit(String id, Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");
        Thesis thesis = thesisService.findThesisByTid(id);
        List<Document> documentList = documentService.findDocumentByItemId(id);
        FileKit.thesisMap = FileKit.clearOrInitMap(FileKit.thesisMap, uid);
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + uid + File.separator + "temp//thesis"));
        model.addAttribute("documentList", documentList);
        model.addAttribute("thesis", thesis);
        model.addAttribute("publishTime", DateKit.formatDateByUnixTime(thesis.getPublishTime(), "yyyy-MM-dd"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + uid);
        return "manager/edit/thesis-edit";
    }

    /**
     * @param id
     * @return 会议内容编辑页面
     */
    @GetMapping("meeting-edit")
    public String meetingEdit(String id, Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");
        Meeting meeting = meetingService.findMeetingById(id);
        List<Document> documentList = documentService.findDocumentByItemId(id);
        FileKit.meetingMap = FileKit.clearOrInitMap(FileKit.meetingMap, uid);
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + uid + File.separator + "temp//meeting"));
        model.addAttribute("documentList", documentList);
        model.addAttribute("meeting", meeting);
        model.addAttribute("startTime", DateKit.formatDateByUnixTime(meeting.getStartTime(), "yyyy-MM-dd HH:mm"));
        model.addAttribute("endTime", DateKit.formatDateByUnixTime(meeting.getEndTime(), "yyyy-MM-dd HH:mm"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + uid);
        return "manager/edit/meeting-edit";
    }

    /**
     * @param id
     * @return 奖励内容编辑页面
     */
    @GetMapping("reward-edit")
    public String rewardEdit(String id, Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");
        Reward reward = rewardService.findRewardByRid(id);
        List<Document> documentList = documentService.findDocumentByItemId(id);
        FileKit.rewardMap = FileKit.clearOrInitMap(FileKit.rewardMap, uid);
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + uid + File.separator + "temp//reward"));
        model.addAttribute("documentList", documentList);
        model.addAttribute("reward", reward);
        model.addAttribute("getTime", DateKit.formatDateByUnixTime(reward.getGetTime(), "yyyy-MM-dd"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + uid);
        return "manager/edit/reward-edit";
    }

    /**
     * @param id
     * @return 奖励内容编辑页面
     */
    @GetMapping("textbook-edit")
    public String textbookEdit(String id, Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");
        Textbook textbook = textbookService.findTextbookById(id);
        List<Document> documentList = documentService.findDocumentByItemId(id);
        FileKit.textbookMap = FileKit.clearOrInitMap(FileKit.textbookMap, uid);
        FileKit.deleteFile(new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + uid + File.separator + "temp//textbook"));
        List<Category> categories = Arrays.asList(Category.values());
        model.addAttribute("categories", categories);
        model.addAttribute("documentList", documentList);
        model.addAttribute("textbook", textbook);
        model.addAttribute("publishTime", DateKit.formatDateByUnixTime(textbook.getPublishTime(), "yyyy-MM"));
        model.addAttribute("key", UUID.randomUUID().toString().replace("-", "") + "-" + uid);
        return "manager/edit/textbook-edit";
    }


    //item内容编辑提交

    /**
     * ajax项目内容编辑提交，并保存
     *
     * @param id            项目id
     * @param startTimeDate 开始时间
     * @param endTimeDate   结束时间
     * @param setTimeDate   建立时间
     * @throws IOException
     */
    @PostMapping("ajax-project-edit-form")
    @ResponseBody
    public JSONObject ajaxProjectEditForm(String key, String id, HttpSession session, Project project, String startTimeDate, String endTimeDate, String setTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //将日期转化为时间戳
        startTimeDate += " 00:00:00";
        endTimeDate += " 00:00:00";
        setTimeDate += " 00:00:00";
        project.setStartTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(startTimeDate)))));
        project.setEndTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(endTimeDate)))));
        project.setSetTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(setTimeDate)))));

        project.setPid(id);
        System.out.println("修改项目测试-------->" + project);
        //以下三种种错误
//        int projectNum = projectService.countProjectByNameHostFromExceptPid(project.getName(), project.getHost(), project.getProjectSource(),project.getPid());
        int projectNum = projectService.countProjectByTopicHostSettimeExceptPid(project.getName(), project.getHost(), project.getSetTime(), project.getPid());
        if (projectNum != 0) {
            json.put("msg", "该项目已被提交！");
            return json;
        }
//        if(projectService.countProjectByTopicExceptPid(project.getTopic(),project.getPid())!=0){
//            json.put("msg", "该项目题目已被提交！");
//            return json;
//        }
        String uid = (String) session.getAttribute("uid");
        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);
        Project preProject = projectService.findProjectByPid(project.getPid());
        project.setMid(preProject.getMid());
        project.setIsAudit(midList1.contains(project.getMid()));

        UserInfo user = userInfoService.findUserByUid(uid);
        //姓名去重，并重新排序
        String member = project.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        nameMap.put(project.getHost(), "");//添加主持人
        if (!nameMap.containsKey(user.getName()) && !project.getIsAudit()) {
            json.put("msg", "此项目与本账号用户无关！");
            return json;
        }
        project.setPeople(member);
        /**
         * 新建项目记录
         */

        project.setState("0");

        List<MultipartFile> projectFileList = FileKit.projectMap.get(key);
        if (!nameMap.containsKey(user.getName()) && project.getIsAudit()) {
            if (CollectionUtils.isEmpty(projectFileList)) {
                json.put("msg", "请先上传审核证明材料！");
                return json;
            }
            project.setState("2");
        }

        project.setUpdateTime(DateKit.getUnixTimeLong());
        projectService.updateProject(project);
        /**
         * 新建用户与项目的关系记录
         */
        //删除之前用户与项目的关系记录
        userItemService.deleteUserItemByItemId(project.getPid());
        //建立新用户与项目的关系记录
        UserItem userItem = new UserItem();
        userItem.setItemId(project.getPid());
        userItem.setItemType("project");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }

        if (projectFileList != null && projectFileList.size() != 0) {
            Project dbProject = projectService.findProjectByPid(id);
            Document document = new Document();
            document.setItemId(project.getPid());
            document.setItemType("project");
            String fileName = "";

            for (MultipartFile file : projectFileList) {
                int i = 0;
                fileName = file.getOriginalFilename();
                List<Document> documentList = documentService.findDocumentByItemId(project.getPid());
                for (Document d : documentList) {
                    if (d.getName().equals(fileName)) i++;
                }
                if (i != 0) {
                    continue;
                }
                document.setName(fileName);
                document.setType(fileName.substring(fileName.lastIndexOf(".")));
                document.setPath("upload" + File.separator + user.getUid() + File.separator + "project" + File.separator + dbProject.getCreateTime() + File.separator + fileName);
                documentService.createDocument(document);
                //文件拷贝与删除
                File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "project", fileName);
                File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "project" + File.separator + dbProject.getCreateTime(), fileName);
                File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "project" + File.separator + dbProject.getCreateTime());
                pathFile.mkdirs();
                FileCopyUtils.copy(tempFile, targetFile);
                FileKit.deleteFile(tempFile);
            }
            FileKit.projectMap.remove(key);
            File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "project");
            FileKit.deleteFile(dirFile);
        }
        json.put("msg", "提交成功待审核！");
        return json;
    }


    /**
     * ajax论文内容编辑提交，并保存
     *
     * @param startPage 起始页码
     * @param endPage   结束页码
     * @throws IOException
     */
    @PostMapping("ajax-thesis-edit-form")
    @ResponseBody
    public JSONObject ajaxThesisEditForm(String publishTimeDate, String key, String id, Thesis thesis, Integer startPage, Integer endPage, HttpSession session) throws IOException {
        JSONObject json = new JSONObject();
        thesis.setTid(id);
        //以下四种错误
        if (thesis.getDossier() == null && thesis.getIssue() == null) {
            json.put("msg", "期和卷至少填一处");
            return json;
        }
        int thesisNum = thesisService.countThesisByHostNameExceptTid(thesis.getHost(), thesis.getName(), thesis.getTid());
        if (thesisNum != 0) {
            json.put("msg", "该项目已被提交！");
            return json;
        }
        String uid = (String) session.getAttribute("uid");
        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);
        Thesis preThesis = thesisService.findThesisByTid(thesis.getTid());
        thesis.setMid(preThesis.getMid());
        thesis.setIsAudit(midList1.contains(thesis.getMid()));

        UserInfo user = userInfoService.findUserByUid(uid);
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put(thesis.getHost(), "");//添加主持人
        //姓名去重，并重新排序
        if (thesis.getPeople() != null && !"".equals(thesis.getPeople())) {
            String member = thesis.getPeople().replace("，", ",").replace("、", ",").trim();
            String[] names = member.split(",");
            String people = "";
            for (String name : names) {
                if (!name.equals("")) {
                    nameMap.put(name, "");
                }
            }
            Iterator iterator1 = nameMap.entrySet().iterator();
            while (iterator1.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator1.next();
                people = people + (String) entry.getKey() + ",";
            }

            thesis.setPeople(member);
        }
        if (!nameMap.containsKey(user.getName())&& !thesis.getIsAudit()) {
            json.put("msg", "此论文与本账号用户无关！");
            return json;
        }
        /**
         * 更新论文记录
         */
        publishTimeDate += " 00:00:00";
        thesis.setPublishTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(publishTimeDate)))));
        thesis.setPageNum(startPage + "-" + endPage);
        thesis.setState("0");

        List<MultipartFile> thesisFileList = FileKit.thesisMap.get(key);
        if (!nameMap.containsKey(user.getName()) && thesis.getIsAudit()) {
            if (CollectionUtils.isEmpty(thesisFileList)) {
                json.put("msg", "请先上传审核证明材料！");
                return json;
            }
            thesis.setState("2");
        }

        thesis.setUpdateTime(DateKit.getUnixTimeLong());
        thesisService.updateThesis(thesis);
        if (thesis.getDossier() == null && thesis.getIssue() != null)
            thesisService.setDossierNull(thesis.getTid());
        if (thesis.getDossier() != null && thesis.getIssue() == null)
            thesisService.setIssueNull(thesis.getTid());
        /**
         * 新建和删除用户与论文的关系记录
         */
        //删除之前的关系记录
        userItemService.deleteUserItemByItemId(thesis.getTid());
        //添加新的关系记录
        UserItem userItem = new UserItem();
        userItem.setItemId(thesis.getTid());
        userItem.setItemType("thesis");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }

        if (thesisFileList != null && thesisFileList.size() != 0) {
            Thesis dbThesis = thesisService.findThesisByTid(id);
            Document document = new Document();
            document.setItemId(thesis.getTid());
            document.setItemType("thesis");
            String fileName = "";
            for (MultipartFile file : thesisFileList) {
                int i = 0;
                fileName = file.getOriginalFilename();
                List<Document> documentList = documentService.findDocumentByItemId(thesis.getTid());
                for (Document d : documentList) {
                    if (d.getName().equals(fileName)) i++;
                }
                if (i != 0) {
                    continue;
                }
                document.setName(fileName);
                document.setType(fileName.substring(fileName.lastIndexOf(".")));
                document.setPath("upload" + File.separator + user.getUid() + File.separator + "thesis" + File.separator + dbThesis.getCreateTime() + File.separator + fileName);
                documentService.createDocument(document);
                //文件拷贝与删除
                File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "thesis", fileName);
                File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "thesis" + File.separator + dbThesis.getCreateTime(), fileName);
                File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "thesis" + File.separator + dbThesis.getCreateTime());
                pathFile.mkdirs();
                FileCopyUtils.copy(tempFile, targetFile);
                FileKit.deleteFile(tempFile);
            }
            FileKit.thesisMap.remove(key);
            File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "thesis");
            FileKit.deleteFile(dirFile);
        }
        json.put("msg", "提交成功待审核！");
        return json;
    }


    /**
     * ajax会议内容编辑提交，并保存
     */
    @PostMapping("ajax-meeting-edit-form")
    @ResponseBody
    public JSONObject ajaxMeetingEditForm(String key, HttpSession session, Meeting meeting, String startTimeDate, String endTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //以下三种种错误
        startTimeDate += ":00";
        endTimeDate += ":00";
        meeting.setStartTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(startTimeDate)))));
        meeting.setEndTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(endTimeDate)))));
        Integer meetingNum = meetingService.countMeetingByNameMeetingTimeExceptId(meeting.getName(), meeting.getStartTime(), meeting.getId());
        if (meetingNum != 0) {
            json.put("msg", "该会议已被提交！");
            return json;
        }
        String uid = (String) session.getAttribute("uid");
        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);
        Meeting preMeeting = meetingService.findMeetingById(meeting.getId());
        meeting.setMid(preMeeting.getMid());
        meeting.setIsAudit(midList1.contains(meeting.getMid()));

        UserInfo user = userInfoService.findUserByUid(uid);
        //姓名去重，并重新排序
        String member = meeting.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        if (!nameMap.containsKey(user.getName())&& !meeting.getIsAudit()) {
            json.put("msg", "此会议与本账号用户无关！");
            return json;
        }
        meeting.setPeople(member);
        /**
         * 新建项目记录
         */

        //保存会议信息
        meeting.setState("0");

        List<MultipartFile> meetingFileList = FileKit.meetingMap.get(key);
        if (!nameMap.containsKey(user.getName()) && meeting.getIsAudit()) {
            if (CollectionUtils.isEmpty(meetingFileList)) {
                json.put("msg", "请先上传审核证明材料！");
                return json;
            }
            meeting.setState("2");
        }


        meeting.setUpdateTime(DateKit.getUnixTimeLong());
        meetingService.updateMeeting(meeting);
        /**
         * 删除和新建用户与项目的关系记录
         */
        //删除旧记录
        userItemService.deleteUserItemByItemId(meeting.getId());
        //新建新记录
        UserItem userItem = new UserItem();
        userItem.setItemId(meeting.getId());
        userItem.setItemType("meeting");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }

        if (meetingFileList != null && meetingFileList.size() != 0) {
            Meeting dbMeeting = meetingService.findMeetingById(meeting.getId());
            Document document = new Document();
            document.setItemId(meeting.getId());
            document.setItemType("meeting");
            String fileName = "";
            for (MultipartFile file : meetingFileList) {
                int i = 0;
                fileName = file.getOriginalFilename();
                List<Document> documentList = documentService.findDocumentByItemId(meeting.getId());
                for (Document d : documentList) {
                    if (d.getName().equals(fileName)) i++;
                }
                if (i != 0) {
                    continue;
                }
                document.setName(fileName);
                document.setType(fileName.substring(fileName.lastIndexOf(".")));
                document.setPath("upload" + File.separator + user.getUid() + File.separator + "meeting" + File.separator + dbMeeting.getCreateTime() + File.separator + fileName);
                documentService.createDocument(document);
                //文件拷贝与删除
                File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "meeting", fileName);
                File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "meeting" + File.separator + dbMeeting.getCreateTime(), fileName);
                File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "meeting" + File.separator + dbMeeting.getCreateTime());
                pathFile.mkdirs();
                FileCopyUtils.copy(tempFile, targetFile);
                FileKit.deleteFile(tempFile);
            }
            FileKit.meetingMap.remove(key);
            File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp//meeting");
            FileKit.deleteFile(dirFile);
        }
        json.put("msg", "提交成功待审核！");
        return json;
    }


    /**
     * ajax奖励编辑提交，并保存
     */
    @PostMapping("ajax-reward-edit-form")
    @ResponseBody
    public JSONObject ajaxRewardEditForm(String key, String id, HttpSession session, Reward reward, String getTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        reward.setRid(id);
        //日期转化为时间戳
        getTimeDate += " 00:00:00";
        reward.setGetTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(getTimeDate)))));

        //姓名去重，并重新排序
        String member = reward.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        reward.setPeople(member);

        //以下两种错误
        int rewardNum = rewardService.countRewardByNamePeopleGetTimeExceptRid(reward.getName(), reward.getPeople(), reward.getGetTime(), reward.getRid());
        if (rewardNum != 0) {
            json.put("msg", "该奖励已被提交！");
            return json;
        }

        String uid = (String) session.getAttribute("uid");
        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);
        Reward preReward = rewardService.findRewardByRid(reward.getRid());
        reward.setMid(preReward.getMid());
        reward.setIsAudit(midList1.contains(reward.getMid()));

        UserInfo user = userInfoService.findUserByUid(uid);
        if (!nameMap.containsKey(user.getName()) && !reward.getIsAudit()) {
            json.put("msg", "此奖励与本账号用户无关！");
            return json;
        }
        /**
         * 更新奖励记录
         */
        //将日期转化为时间戳
        reward.setGetTime(Long.parseLong(String.valueOf(DateKit.getUnixTimeByDate(DateKit.dateFormat(getTimeDate)))));
        reward.setState("0");

        List<MultipartFile> rewardFileList = FileKit.rewardMap.get(key);
        if (!nameMap.containsKey(user.getName()) && reward.getIsAudit()) {
            if (CollectionUtils.isEmpty(rewardFileList)) {
                json.put("msg", "请先上传审核证明材料！");
                return json;
            }
            reward.setState("2");
        }

        reward.setUpdateTime(DateKit.getUnixTimeLong());
        rewardService.updateReward(reward);
        /**
         * 新建用户与奖励的关系记录
         */
        //删除记录
        userItemService.deleteUserItemByItemId(reward.getRid());
        //新建记录
        UserItem userItem = new UserItem();
        userItem.setItemId(reward.getRid());
        userItem.setItemType("reward");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }

        if (rewardFileList != null && rewardFileList.size() != 0) {
            Reward dbReward = rewardService.findRewardByRid(id);
            Document document = new Document();
            document.setItemId(reward.getRid());
            document.setItemType("reward");
            String fileName = "";
            for (MultipartFile file : rewardFileList) {
                int i = 0;
                fileName = file.getOriginalFilename();
                List<Document> documentList = documentService.findDocumentByItemId(reward.getRid());
                for (Document d : documentList) {
                    if (d.getName().equals(fileName)) i++;
                }
                if (i != 0) {
                    continue;
                }
                document.setName(fileName);
                document.setType(fileName.substring(fileName.lastIndexOf(".")));
                document.setPath("upload" + File.separator + user.getUid() + File.separator + "reward" + File.separator + dbReward.getCreateTime() + File.separator + fileName);
                documentService.createDocument(document);
                //文件拷贝与删除
                File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "reward", fileName);
                File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "reward" + File.separator + dbReward.getCreateTime(), fileName);
                File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "reward" + File.separator + dbReward.getCreateTime());
                pathFile.mkdirs();
                FileCopyUtils.copy(tempFile, targetFile);
                FileKit.deleteFile(tempFile);
            }
            FileKit.rewardMap.remove(key);
            File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "reward");
            FileKit.deleteFile(dirFile);
        }
        json.put("msg", "提交成功待审核！");
        return json;
    }


    /**
     * ajax教材编辑提交，并保存
     */
    @PostMapping("ajax-textbook-edit-form")
    @ResponseBody
    public JSONObject ajaxTextbookEditForm(String key, HttpSession session, Textbook textbook, String publishTimeDate) throws IOException {
        JSONObject json = new JSONObject();
        //以下2种错误
        Integer textBookNum = textbookService.countTextBookByISBNExceptId(textbook.getIsbn(), textbook.getId());
        if (textBookNum != 0) {
            json.put("msg", "该教材/专著已被提交！");
            return json;
        }
        String uid = (String) session.getAttribute("uid");
        //获取用户是否具有审核权限
        List<Integer> midList1 = userMajorService.findMidListByUidAndPower(uid, 1);
        Textbook preTextBook = textbookService.findTextbookById(textbook.getId());
        textbook.setMid(preTextBook.getMid());
        textbook.setIsAudit(midList1.contains(textbook.getMid()));

        UserInfo user = userInfoService.findUserByUid(uid);
        //姓名去重，并重新排序
        String member = textbook.getPeople().replace("，", ",").replace("、", ",").trim();
        String[] names = member.split(",");
        Map<String, String> nameMap = new HashMap<>();
        String people = "";
        for (String name : names) {
            if (!name.equals("")) {
                nameMap.put(name, "");
            }
        }
        Iterator iterator1 = nameMap.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            people = people + (String) entry.getKey() + ",";
        }
        if (!nameMap.containsKey(user.getName()) && !textbook.getIsAudit()) {
            json.put("msg", "此教材/专著与本账号用户无关！");
            return json;
        }
        textbook.setPeople(member);
        /**
         * 新建项目记录
         */

        //将日期转化为时间戳
        publishTimeDate += "-01 00:00:00";
        Date date = DateKit.dateFormat(publishTimeDate);
        Calendar n = Calendar.getInstance();
        n.setTime(date);
        int month = n.get(Calendar.MONTH);
        n.set(Calendar.MONTH, month);
        textbook.setPublishTime(DateKit.getUnixTimeLong(n.getTime()));
        textbook.setState("0");

        List<MultipartFile> textbookFileList = FileKit.textbookMap.get(key);

        if (!nameMap.containsKey(user.getName()) && textbook.getIsAudit()) {
            if (CollectionUtils.isEmpty(textbookFileList)) {
                json.put("msg", "请先上传审核证明材料！");
                return json;
            }
            textbook.setState("2");
        }
        textbook.setUpdateTime(DateKit.getUnixTimeLong());
        textbookService.updateTextbook(textbook);
        /**
         * 新建用户与项目的关系记录
         */
        //删除旧记录
        userItemService.deleteUserItemByItemId(textbook.getId());
        //添加新纪录
        UserItem userItem = new UserItem();
        userItem.setItemId(textbook.getId());
        userItem.setItemType("textbook");
        Iterator iterator2 = nameMap.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            List<UserInfo> userInfoList = userInfoService.findUserByName((String) entry.getKey());
            if (userInfoList.size() != 0) {
                userItem.setUid(userInfoList.get(0).getUid());
                userItemService.createUserItem(userItem);
            }
        }

        if (textbookFileList != null && textbookFileList.size() != 0) {
            Textbook dbTextbook = textbookService.findTextbookById(textbook.getId());
            Document document = new Document();
            document.setItemId(textbook.getId());
            document.setItemType("textbook");
            String fileName = "";
            for (MultipartFile file : textbookFileList) {
                int i = 0;
                fileName = file.getOriginalFilename();
                List<Document> documentList = documentService.findDocumentByItemId(textbook.getId());
                for (Document d : documentList) {
                    if (d.getName().equals(fileName)) i++;
                }
                if (i != 0) {
                    continue;
                }
                document.setName(fileName);
                document.setType(fileName.substring(fileName.lastIndexOf(".")));
                document.setPath("upload" + File.separator + user.getUid() + File.separator + "textbook" + File.separator + dbTextbook.getCreateTime() + File.separator + fileName);
                documentService.createDocument(document);
                //文件拷贝与删除
                File tempFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "textbook", fileName);
                File targetFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "textbook" + File.separator + dbTextbook.getCreateTime(), fileName);
                File pathFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "textbook" + File.separator + dbTextbook.getCreateTime());
                pathFile.mkdirs();
                FileCopyUtils.copy(tempFile, targetFile);
                FileKit.deleteFile(tempFile);
            }
            FileKit.textbookMap.remove(key);
            File dirFile = new File(UploadUtil.getUploadFilePath() + File.separator + "upload" + File.separator + user.getUid() + File.separator + "temp" + File.separator + "textbook");
            FileKit.deleteFile(dirFile);
        }
        json.put("msg", "提交成功待审核！");
        return json;
    }


}
