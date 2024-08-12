package me.nald.blog.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.data.dto.AccountDtoTest;
import me.nald.blog.data.dto.AccountRequest;
import me.nald.blog.data.dto.AccountResonseDto;
import me.nald.blog.data.dto.AccountStatusRequestDto;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.AccountLog;
import me.nald.blog.data.entity.Password;
import me.nald.blog.exception.InvalidParameterException;
import me.nald.blog.exception.NotAllowedMethodException;
import me.nald.blog.exception.NotFoundException;
import me.nald.blog.repository.AccountLogRepository;
import me.nald.blog.repository.AccountQueryRepository;
import me.nald.blog.repository.AccountRepository;
import me.nald.blog.response.ResponseCode;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.util.CommonUtils;
import me.nald.blog.util.HttpServletRequestUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;
  private final AccountQueryRepository accountQueryRepository;
  private final AccountLogRepository accountLogRepository;


  private static final Logger logger = LogManager.getLogger(AccountService.class);

  public List<Account> findMembers() {
    return accountRepository.findAll();
  }

  public Account findMemberByAccountId(String accountId) {

    return accountRepository.findByAccountId(accountId);
  }

  public List<AccountDtoTest> getTest() {
    return accountQueryRepository.findByTest();
  }


  public ResponseObject getUserList() {
    ResponseObject response = new ResponseObject();
    HashMap<String, Object> data = new HashMap<>();
    List<AccountResonseDto.UserInfo> list = accountRepository.findAll().stream().map(AccountResonseDto.UserInfo::new).collect(Collectors.toList());
    data.put("list", list);
    data.put("total", list.size());
    response.putData(data);
    return response;

  }

  @Transactional
  public ResponseObject getLogin(AccountRequest accountInfo) {
    ResponseObject response = new ResponseObject();
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() ->
            new NotFoundException(logger, ResponseCode.USER_NOT_FOUND));
    Optional.ofNullable(accountInfo.getPassword()).orElseThrow(() -> new InvalidParameterException(logger, ResponseCode.PASSWORD_NOT_MATCH));

    Account user = accountRepository.findByAccountId(accountInfo.getAccountId());

    int statusCode = 0;
    HashMap<String, Object> data = new HashMap<>();

    String ipAddr = HttpServletRequestUtil.getRemoteIP(request);
    boolean isLocal = ipAddr.equals("127.0.0.1") || ipAddr.equals("localhosts");

    if (user == null) throw new NotFoundException(logger, ResponseCode.USER_NOT_FOUND);

    if (!user.getPassword().isMatched(accountInfo.getPassword())) {
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

    response.putData(data);
    return response;
  }

  @Transactional
  public ResponseObject editPassword(AccountRequest accountInfo) {
    Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() ->
            new NotFoundException(logger, ResponseCode.USER_NOT_FOUND));
    Optional.ofNullable(accountInfo.getPassword()).orElseThrow(() -> new InvalidParameterException(logger, ResponseCode.PASSWORD_NOT_MATCH));

    Account user = accountRepository.findByAccountId(accountInfo.getAccountId());
    Password password = Password.builder()
            .password(accountInfo.getPassword())
            .build();

    user.setPassword(password);
    accountRepository.save(user);
    return new ResponseObject();
  }

  @Transactional
  public ResponseObject createUser(AccountRequest accountInfo) {
    Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() ->
            new NotFoundException(logger, ResponseCode.INVALID_PARAMETER));
    Optional.ofNullable(accountInfo.getPassword()).orElseThrow(() -> new InvalidParameterException(logger, ResponseCode.INVALID_PARAMETER));

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
      accountRepository.save(account);
    } else {
      throw new NotAllowedMethodException(logger, ResponseCode.ID_DUPLICATE);
    }
    return new ResponseObject();
  }

  @Transactional
  public ResponseObject editUser(AccountRequest accountInfo) {
    Optional.ofNullable(accountInfo.getAccountId()).orElseThrow(() ->
            new NotFoundException(logger, ResponseCode.USER_NOT_FOUND));
    Account user = accountRepository.findByAccountId(accountInfo.getAccountId());
    user.setAccountName(accountInfo.getAccountName());
    user.setEmail(accountInfo.getEmail());
    user.setAuthority(Integer.parseInt(accountInfo.getAuthority()));
    accountRepository.save(user);
    return new ResponseObject();
  }


  @Transactional
  public ResponseObject changeStatus(AccountStatusRequestDto accountStatusRequest) {

    accountStatusRequest.getUserIds().stream().forEach(s -> {
      Account user = accountRepository.findByAccountId(s);
      user.setStatus(accountStatusRequest.getStatus());
      accountRepository.save(user);
    });

    return new ResponseObject();
  }


}

