package me.nald.blog.service;


import lombok.RequiredArgsConstructor;
import me.nald.blog.data.dto.AccountDtoTest;
import me.nald.blog.data.dto.AccountRequest;
import me.nald.blog.data.dto.AccountResonseDto;
import me.nald.blog.data.dto.AccountStatusRequestDto;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.AccountLog;
import me.nald.blog.data.entity.Password;
import me.nald.blog.exception.ErrorException;
import me.nald.blog.exception.ErrorSpec;
import me.nald.blog.exception.Errors;
import me.nald.blog.repository.AccountLogRepository;
import me.nald.blog.repository.AccountQueryRepository;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.response.Response;
import me.nald.blog.util.CommonUtils;
import me.nald.blog.util.HttpServletRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.nald.blog.exception.ErrorSpec.DuplicatedId;
import static me.nald.blog.exception.ErrorSpec.UserNotFound;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountQueryRepository accountQueryRepository;
    private final AccountLogRepository accountLogRepository;

    public List<Account> findMembers() {
        return accountRepository.findAll();
    }

    public Account findMemberByAccountId(String accountId) {

        return accountRepository.findByAccountId(accountId);
    }

    public List<AccountDtoTest> getTest() {
       return  accountQueryRepository.findByTest();
    }


    public Response.CommonRes getUserList() {

        HashMap<String, Object> data = new HashMap<>();
        List<AccountResonseDto.UserInfo> list = accountRepository.findAll().stream().map(AccountResonseDto.UserInfo::new).collect(Collectors.toList());
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

        if (user == null) {
            statusCode = 401;
            data.put("error", "incorrect user");
        } else if (!user.getPassword().isMatched(accountInfo.getPassword())) {
            statusCode = 401;
            user.setLoginFailCnt(user.getLoginFailCnt() + 1);
            data.put("error", "incorrect password failed: " + user.getLoginFailCnt());
            if (user.getLoginFailCnt() > 4) {
                user.setStatus(1);
                data.put("error", "your id has been blocked");
            }
        } else if (user.getStatus() != 0) {
            data.put("error", "the account is not able to login");
        } else {
            statusCode = 200;
            user.setLoginFailCnt(0);
            user.setRecentLoginDt(new Timestamp(System.currentTimeMillis()));
            data.put("access_token", CommonUtils.getJWTToken(user));
            data.put("message", "succeeded");
            data.put("accountId", user.getAccountId());
            data.put("accountName", user.getAccountName());
        }
        if (!isLocal) {
            if (statusCode == 200) {
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
    public Response.CommonRes editPassword(AccountRequest accountInfo) {
        int statusCode = 200;
        HashMap<String, Object> data = new HashMap<>();
        data.put("message", "succeeded");
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

    @Transactional
    public Response.CommonRes createUser(AccountRequest accountInfo) {
        int statusCode = 200;
        HashMap<String, Object> data = new HashMap<>();

        Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Optional.ofNullable(accountInfo.getPassword()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Account user = accountRepository.findByAccountId(accountInfo.getAccountId());

        if (user == null) {
            Password password = Password.builder()
                    .password(accountInfo.getPassword())
                    .build();
            Account account = Account.createAccount(
                    accountInfo.getAccountId(),
                    accountInfo.getAccountName(),
                    accountInfo.getEmail(),
                    Integer.parseInt(accountInfo.getAuthority()),
                    password,
                    0
            );
            data.put("message", "succeeded");
            accountRepository.save(account);
        } else {
            throw Errors.of(DuplicatedId, "Id is duplicated");
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
        Account user = accountRepository.findByAccountId(accountInfo.getAccountId());

        if (user != null) {
            user.setAccountName(accountInfo.getAccountName());
            user.setEmail(accountInfo.getEmail());
            user.setAuthority(Integer.parseInt(accountInfo.getAuthority()));
//            user.setStatus();
            data.put("message", "succeeded");
            accountRepository.save(user);
        } else {
            throw Errors.of(UserNotFound, "user not found");
        }

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(statusCode)
                .data(data)
                .build();

        return result;
    }


    @Transactional
    public Response.CommonRes changeStatus(AccountStatusRequestDto accountStatusRequest) {
        int statusCode = 200;
        HashMap<String, Object> data = new HashMap<>();

        accountStatusRequest.getUserIds().stream().forEach(s -> {
            Account user = accountRepository.findByAccountId(s);
            System.out.println("날드 유저는 " + user);
            user.setStatus(accountStatusRequest.getStatus());
            System.out.println("날드 유저는 " + user);
            accountRepository.save(user);
        });

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(statusCode)
                .data(data)
                .build();

        return result;
    }


}

