package org.example.expert.domain.common.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.expert.domain.common.dto.AuthUser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLoggingAspect {

    private final ObjectMapper objectMapper;

    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..)) || " +
            "execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
    public void adminApiByMethodSignature() {
    }

    @Pointcut("@annotation(org.example.expert.domain.common.annotation.AdminLogger)")
    public void adminApiByAnnotation() {
    }

    @Around("adminApiByAnnotation()")  // 위에서 정의한 Pointcut 사용
    public Object logAdminApi(ProceedingJoinPoint joinPoint) throws Throwable {

        // 현재 HTTP 요청 가져오기
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 메서드 정보 가져오기
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 요청 시각
        String requestTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // API 요청 URL
        String requestUrl = request.getRequestURI();

        // HTTP 메서드 (GET, POST, DELETE 등)
        String httpMethod = request.getMethod();

        // 사용자 ID 추출
        Long userId = extractUserId(joinPoint);

        // 요청 본문 추출 (RequestBody)
        String requestBody = extractRequestBody(joinPoint);

        log.info("====================================");
        log.info("Admin API 호출 시작");
        log.info("====================================");
        log.info("요청 시각: {}", requestTime);
        log.info("사용자 ID: {}", userId != null ? userId : "인증 정보 없음");
        log.info("HTTP 메서드: {}", httpMethod);
        log.info("요청 URL: {}", requestUrl);
        log.info("실행 메서드: {}.{}()",
                method.getDeclaringClass().getSimpleName(),
                method.getName());
        log.info("요청 본문: {}", requestBody);

        Object result = null;
        long startTime = System.currentTimeMillis();

        try {
            // 실제 컨트롤러 메서드 실행
            // 이 부분에서 원본 비즈니스 로직이 수행됨
            result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // 응답 본문
            String responseBody = convertToJson(result);

            log.info("------------------------------------");
            log.info("응답 본문: {}", responseBody);
            log.info("실행 시간: {}ms", executionTime);
            log.info("====================================");
            log.info("Admin API 호출 종료 (성공)");
            log.info("====================================");

            return result;

        } catch (Exception e) {

            long executionTime = System.currentTimeMillis() - startTime;

            log.error("------------------------------------");
            log.error("실행 시간: {}ms", executionTime);
            log.error("에러 타입: {}", e.getClass().getSimpleName());
            log.error("에러 메시지: {}", e.getMessage());
            log.error("====================================");
            log.error("Admin API 호출 종료 (실패)");
            log.error("====================================");
            throw e;
        }
    }


    private Long extractUserId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg instanceof AuthUser) {
                return ((AuthUser) arg).getId();
            }
        }

        // AuthUser를 찾지 못한 경우
        log.warn("메서드 파라미터에서 AuthUser를 찾을 수 없습니다.");
        return null;
    }

    private String extractRequestBody(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            // null 체크
            if (arg == null) {
                continue;
            }

            // 제외할 타입들
            if (arg instanceof AuthUser ||
                    arg instanceof HttpServletRequest ||
                    arg instanceof Long ||
                    arg instanceof Integer ||
                    arg instanceof String) {
                continue;
            }

            // DTO 객체를 JSON으로 변환
            return convertToJson(arg);
        }

        return "없음";
    }

    private String convertToJson(Object object) {
        if (object == null) {
            return "없음";
        }

        try {
            // ObjectMapper로 JSON 변환
            // 장점: 가독성 좋고, 중첩 객체도 잘 처리됨
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            // JSON 변환 실패 시 toString() 사용
            log.warn("JSON 변환 실패: {}", e.getMessage());
            return object.toString();
        }
    }
}