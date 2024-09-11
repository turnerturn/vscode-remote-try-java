package com.mycompany.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RestController
public class H2Controller {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/h2/users")
    public String createUser(@RequestBody UserDto user) {
        String sql = "CREATE USER IF NOT EXISTS ? PASSWORD ?";
        jdbcTemplate.update(sql, user.getUsername(), user.getPassword());
        return "User created";
    }

    @PostMapping("/h2/users/{username}/password")
    public String alterUserPassword(@PathVariable("username") String username, @RequestBody String password) {
        String sql = "ALTER USER ? SET PASSWORD ?";
        jdbcTemplate.update(sql, username, password);
        return "User password altered";
    }
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
class UserDto {
    private String username;
    private String password;
}