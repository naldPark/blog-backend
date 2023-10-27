package me.nald.blog.service.store;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.nald.blog.util.HttpServletRequestUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@AllArgsConstructor
@Component("accountStore")
public class AccountStore {
    // 사용자 로그인 상태 관리


    Map<String, Integer> mailCountTracker = new ConcurrentHashMap<>();
    // 멀티스레드 환경에서 Hashmap은 synchronized가 없으면 안정성과 신뢰성 보장이 되지 않음
    // ConcurrentHashMap의 경우 Map의 일부분에만 block/unblock 처리가 적용되기 때문에 thread-safe라는 특징과 성능까지 고려된 클래스

    // 사용자가 메일 보낸 횟수 누적
    public boolean checkMailSentCount(HttpServletRequest request) {
        String ipAddr = HttpServletRequestUtil.getRemoteIP(request);
        boolean isLocal = ipAddr.equals("127.0.0.1") || ipAddr.equals("localhosts");
         //computeIfAbsent: ipAddr이라는 Key가 부재중인 경우에 한해 value에 v-> 1을 넣은 map을 추가
        mailCountTracker.computeIfAbsent(ipAddr,  v -> 1);
        if(!isLocal && mailCountTracker.get(ipAddr) > 4){
            return false;
        }else{
            return true;
        }}

    public void mailCountReset(){
        mailCountTracker = new ConcurrentHashMap<>();
    }

}
