package com.kboticketing.kboticketing.service;

import com.kboticketing.kboticketing.dao.UserMapper;
import com.kboticketing.kboticketing.domain.User;
import com.kboticketing.kboticketing.dto.EmailRequestDto;
import com.kboticketing.kboticketing.dto.UserDto;
import com.kboticketing.kboticketing.dto.VerificationCodeDto;
import com.kboticketing.kboticketing.utils.EmailUtils;
import com.kboticketing.kboticketing.utils.exception.CustomException;
import com.kboticketing.kboticketing.utils.exception.ErrorCode;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import static com.kboticketing.kboticketing.utils.NumberUtils.createRandomNumber;
import static com.kboticketing.kboticketing.utils.exception.ErrorCode.EMAIL_ALREADY_EXISTS;
import static com.kboticketing.kboticketing.utils.exception.ErrorCode.PASSWORD_MISMATCH;

/**
 * @author hazel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;
    private final EmailUtils emailUtil;
    private final RedisTemplate<String, String> redisTemplate;

    public void signUp(UserDto userDto) {
        Boolean hasKey = redisTemplate.hasKey(userDto.getEmail());
        if (Boolean.FALSE.equals(hasKey)) {
            throw new CustomException(ErrorCode.REQUEST_VERIFICATION_FIRST);
        }

        if (!userDto.getPassword()
                    .equals(userDto.getConfirmedPassword())) {
            throw new CustomException(PASSWORD_MISMATCH);
        }

        User registeredUser = userMapper.selectByEmail(userDto.getEmail());
        if (registeredUser != null) {
            throw new CustomException(EMAIL_ALREADY_EXISTS);
        }

        User user = userDto.toUser();
        userMapper.insert(user);
    }

    public void sendVerificationCode(EmailRequestDto emailRequestDto) {
        Boolean hasKey = redisTemplate.hasKey(emailRequestDto.getEmail());
        if (hasKey) {
            throw new CustomException(ErrorCode.FREQUENT_VERIFICATION_REQUEST);
        }

        String verificationCode = createRandomNumber();
        log.info("{}'s verification code  = {}", emailRequestDto.getEmail(), verificationCode);

        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(emailRequestDto.getEmail(), verificationCode, 5,
            TimeUnit.MINUTES);

        emailUtil.sendEmail(verificationCode, emailRequestDto.getEmail());
    }

    public void checkVerificationCode(VerificationCodeDto verificationCodeDto) {
        Boolean hasKey = redisTemplate.hasKey(verificationCodeDto.getEmail());
        if (Boolean.FALSE.equals(hasKey)) {
            throw new CustomException(ErrorCode.REQUEST_VERIFICATION_FIRST);
        }

        String storedVerificationCode = redisTemplate.opsForValue()
                                                     .get(verificationCodeDto.getEmail());
        if (!Objects.equals(verificationCodeDto.getVerificationCode(), storedVerificationCode)) {
            throw new CustomException(ErrorCode.WRONG_VERIFICATION_CODE);
        }
    }
}


