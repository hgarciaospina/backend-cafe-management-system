package com.inn.cafe.pojo;

import jakarta.persistence.*;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.io.Serializable;
@NamedQuery(
        name = "User.findByEmailId",
        query = "select u from User u where u.email=:email")

@NamedQuery(
        name = "User.getAllUser",
        query = "select new com.inn.cafe.wrapper.UserWrapper(U.id,U.name,U.email,U.contactNumber,U.status) from User U where U.role='user'")

@NamedQuery(name = "User.updateStatus",
        query = "update User U set U.status=:status where U.id=:id")

@NamedQuery(name = "User.getAllAdmin",
        query = "select U.email from User U where U.role='admin'")

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name = "user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "contactNumber")
    private String contactNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "role")
    private String role;
}