package me.nald.blog.service;


import lombok.RequiredArgsConstructor;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.persistence.entity.Password;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.ErrorException;
import me.nald.blog.exception.ErrorSpec;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.response.CommonResponse;
import me.nald.blog.response.Response;
import me.nald.blog.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> findMembers() {
        return accountRepository.findAll();
    }

    public Account findMemberByAccountId(String accountId) {

        return accountRepository.findByAccountId(accountId);
    }

    public CommonResponse getUserList() {

        HashMap<String, Object> map = new HashMap<>();
        CommonResponse commonResponse = new CommonResponse();

        AccountDto.getUserList getUserList = AccountDto.getUserList.builder()
                .statusCode(200)
                .list(map)
                .build();

        commonResponse.putResult(getUserList);
        return commonResponse;

    }

    public Response.CommonRes getLogin(AccountDto.LoginInfo loginInfo) {

        Optional.ofNullable(loginInfo.getAccountId()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Optional.ofNullable(loginInfo.getPassword()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));

        Account user = accountRepository.findByAccountId(loginInfo.getAccountId());

        int statusCode;
        HashMap<String, Object> data = new HashMap<>();

        if(user==null){
            statusCode = 401;
            data.put("error", "incorrect password");
        }else if(!user.getPassword().isMatched(loginInfo.getPassword())){
            statusCode = 401;
            data.put("error", "incorrect password");
        }else{
            statusCode = 200;
            data.put("access_token", Util.getJWTToken(user));
            data.put("message", "succeeded");
            data.put("accountId", user.getAccountId());
            data.put("accountName", user.getAccountName());
        }

        Response.CommonRes result = Response.CommonRes.builder()
                .statusCode(statusCode)
                .data(data)
                .build();

        return result;
    }

    @Transactional
    public Response.CommonRes editUser(AccountDto.LoginInfo loginInfo) {
        int statusCode = 200;
        HashMap<String, Object> data = new HashMap<>();

        Optional.ofNullable(loginInfo.getAccountId()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Optional.ofNullable(loginInfo.getPassword()).orElseThrow(() -> ErrorException.of(ErrorSpec.InvalidParameterValue));
        Account user = accountRepository.findByAccountId(loginInfo.getAccountId());


        Password password = Password.builder()
                .password(loginInfo.getPassword())
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

