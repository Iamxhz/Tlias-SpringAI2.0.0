package com.xhz;

import com.xhz.mapper.EmpMapper;
import com.xhz.pojo.Emp;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class TliasApplicationTests {

    @Autowired
    private EmpMapper empMapper;

    @Test
    public void testGenJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 10);
        claims.put("username", "itheima");

        String jwt = Jwts.builder().signWith(SignatureAlgorithm.HS256, "aXRjYXN0")
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + 12 * 3600 * 1000))
                .compact();

        System.out.println(jwt);
    }
    @Test
    public void testParseJwt() {
        Claims claims = Jwts.parser().setSigningKey("aXRjYXN0")
                .parseClaimsJws("eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MTAsInVzZXJuYW1lIjoiaXRoZWltYSIsImV4cCI6MTc4MDY5MDM0MX0.w4_dq4XVQKiOTtu0mPUzQFYo8g0f9pxaBsUFZXauFFE")
                .getBody();
        System.out.println(claims);
    }
    @Test
    public void genJwt(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 10);
        claims.put("username", "itheima");

        String jwt = Jwts.builder().signWith(SignatureAlgorithm.HS256, "aXRjYXN0")
                .addClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 1000)) //有效期60s
                .compact();
        System.out.println(jwt);
        //输出结果：eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwiZXhwIjoxNjczMDA5NzU0fQ.RcVIR65AkGiax-ID6FjW60eLFH3tPTKdoK7UtE4A1ro
    }

    @Test
    public void parseJwt(){
        Claims claims = Jwts.parser()
                .setSigningKey("aXRjYXN0")//指定签名密钥
                .parseClaimsJws("eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MTAsInVzZXJuYW1lIjoiaXRoZWltYSIsImV4cCI6MTc4MDY0NzQ1N30.wxL_A7oUxQnVuf9i1cZIDK8pNQ5Y-aw0T_bsc3D-yws")
                .getBody();

        System.out.println(claims);
    }
}
