package io.javabrains.springsecurityjwt;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

@Service
public class MyUserDetailsService implements UserDetailsService {

    public String hashPassword(String s) throws UsernameNotFoundException
    {
        String p="";
        Connection connection;
        Statement statement;
        ResultSet resultSet;
        Boolean exists=false;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
            statement=connection.createStatement();
            resultSet=statement.executeQuery("select * from users");
            while(resultSet.next()){
                if(resultSet.getString(1).equals(s)) {
                    p = resultSet.getString(2);
                    exists=true;
                }
            }
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(exists){
            return p;
        }
        else
        {
            throw new UsernameNotFoundException("Username not found");
        }
    }
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        String p="";
        Connection con;
        Statement st;
        ResultSet rs;
        Boolean exists=false;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            con= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
            st=con.createStatement();
            rs=st.executeQuery("select * from users");
            while(rs.next()){
                if(rs.getString(1).equals(s)) {
                    p = rs.getString(2);
                    exists=true;
                }
            }
            con.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(exists){
            return new User(s,p,new ArrayList<>());
        }
        else
        {
            throw new UsernameNotFoundException("Username not found");
        }

    }
}