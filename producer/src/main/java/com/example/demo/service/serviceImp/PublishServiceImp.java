package com.example.demo.service.serviceImp;


import com.example.demo.configuration.ResponseMessage;
import com.example.demo.mapper.FIleMapper;
import com.example.demo.pojo.Comment;
import com.example.demo.pojo.Question;
import com.example.demo.service.service.PublishService;
import com.example.demo.service.service.QuestionPublishToEsService;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class PublishServiceImp implements PublishService {

    @Resource
    StringRedisTemplate template;

    @Resource
    RocketMQTemplate rocketMQTemplate;

    @Resource
    FIleMapper mapper;

    @Autowired
    QuestionPublishToEsService questionPublishToEsService;

    @Override
    public ResponseMessage publishQuestion(Question question, List<String> files) {
        try {
            Date date=new Date();
            String id=String.valueOf(date.getTime())+"l";
            template.opsForHash().put("question_like", String.valueOf(id), "0");
            template.opsForZSet().incrementScore("question_contribute", question.getUser_id(), 1);
            mapper.publishQuestion(question,id);
            String[] tags = question.getQuestion_tags().split(",");
            for (String tag : tags) {
                template.opsForZSet().incrementScore("question_tags", tag, 1);
            }

            template.opsForList().leftPush("pic_list",id);
            mapper.addPic(id, files);
            questionPublishToEsService.publishQuestion(question);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseMessage.success();
    }

    @Override
    public ResponseMessage publishQuestion(Question question) {
        Date date=new Date();
        String id=String.valueOf(date.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        question.setTime(sdf.format(date));
        question.setQuestion_id(id);
        System.out.println(question.getQuestion_id());
        questionPublishToEsService.publishQuestion(question);
        template.opsForHash().put("question_like", id, "0");
        template.opsForZSet().incrementScore("question_contribute", question.getUser_id(), 1);
        mapper.publishQuestion(question,id);
        String[] tags = question.getQuestion_tags().split(",");
        for (String tag : tags) {
            template.opsForZSet().incrementScore("question_tags", tag, 1);
        }

        return ResponseMessage.success();
    }

    @Override
    public ResponseMessage publishComment(Comment comment) {
        try{
            Long commentId = template.boundValueOps("CommentId").increment(1);
            comment.setComment_id(commentId.intValue());
            mapper.publishComment(comment);
            mapper.commentIncrement(comment.getQuestion_id());
            questionPublishToEsService.publishComment(comment);
        }catch (Exception e){
            e.printStackTrace();
        }
//        rocketMQTemplate.asyncSend("CommentPublishService", comment, new SendCallback() {
//            @Override
//            public void onSuccess(SendResult sendResult) {
//
//            }
//
//            @Override
//            public void onException(Throwable throwable) {
//                System.out.println("error");
//            }
//        });
        return ResponseMessage.success();
    }


}
