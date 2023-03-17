package me.nald.blog.service;


import lombok.RequiredArgsConstructor;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.dto.StorageDto;
import me.nald.blog.data.model.AccountRequest;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.AccountLog;
import me.nald.blog.data.persistence.entity.Password;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.ErrorException;
import me.nald.blog.exception.ErrorSpec;
import me.nald.blog.repository.AccountLogRepository;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.response.CommonResponse;
import me.nald.blog.response.Response;
import me.nald.blog.util.HttpServletRequestUtil;
import me.nald.blog.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountLogRepository accountLogRepository;

    public List<Account> findMembers() {
        return accountRepository.findAll();
    }

    public Account findMemberByAccountId(String accountId) {

        return accountRepository.findByAccountId(accountId);
    }

    public Response.CommonRes getUserList() {

        HashMap<String, Object> data = new HashMap<>();
        List<AccountDto.UserInfo> list = accountRepository.findAll().stream().map(AccountDto.UserInfo::new).collect(Collectors.toList());
        data.put("list", list);
        data.put("total", list.size());
        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(200)
                .data(data)
                .build();

        return result;

    }
    @Transactional
    public Response.CommonRes getLogin(AccountRequest accountInfo) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Optional.ofNullable(accountInfo.getPassword()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));

        Account user = accountRepository.findByAccountId(accountInfo.getAccountId());

        int statusCode = 0;
        HashMap<String, Object> data = new HashMap<>();

        String ipAddr = HttpServletRequestUtil.getRemoteIP(request);
        boolean isLocal = ipAddr.equals("127.0.0.1") || ipAddr.equals("localhosts");

        if(user==null){
            statusCode = 401;
            data.put("error", "incorrect user");
        }else if(!user.getPassword().isMatched(accountInfo.getPassword())){
            statusCode = 401;
            user.setLoginFailCnt(user.getLoginFailCnt()+0);
            data.put("error", "incorrect password failed: "+user.getLoginFailCnt());
            if(user.getLoginFailCnt() > 4){
                user.setStatus(1);
                data.put("error", "your id has been blocked");
            }
        }else if(user.getStatus() != 0){
            data.put("error", "the account is not able to login");
        }else{
            statusCode = 200;
            user.setLoginFailCnt(0);
            user.setRecentLoginDt(new Timestamp(System.currentTimeMillis()));
            data.put("access_token", Util.getJWTToken(user));
            data.put("message", "succeeded");
            data.put("accountId", user.getAccountId());
            data.put("accountName", user.getAccountName());
        }
        if(!isLocal){
            if(statusCode == 200){
                user.setRecentLoginDt(new Timestamp(System.currentTimeMillis()));
                AccountLog accountLog = AccountLog.createLog(user, ipAddr);
                accountLogRepository.save(accountLog);
            }
            accountRepository.save(user);
        }
        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(statusCode)
                .data(data)
                .build();

        return result;
    }

    @Transactional
    public Response.CommonRes editUser(AccountRequest accountInfo) {
        int statusCode = 200;
        HashMap<String, Object> data = new HashMap<>();

        Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Optional.ofNullable(accountInfo.getPassword()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Account user = accountRepository.findByAccountId(accountInfo.getAccountId());


        Password password = Password.builder()
                .password(accountInfo.getPassword())
                .build();

        user.setPassword(password);
        accountRepository.save(user);

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(statusCode)
                .data(data)
                .build();

        return result;
    }


}

