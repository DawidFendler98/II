package io.javabrains.springsecurityjwt;


import io.javabrains.springsecurityjwt.authentication.AuthenticationRequest;
import io.javabrains.springsecurityjwt.authentication.AuthenticationResponse;
import io.javabrains.springsecurityjwt.filters.JwtRequestFilter;
import io.javabrains.springsecurityjwt.models.Lists;
import io.javabrains.springsecurityjwt.models.Tables;
import io.javabrains.springsecurityjwt.models.Task;
import io.javabrains.springsecurityjwt.models.User;
import io.javabrains.springsecurityjwt.util.JwtUtil;
import io.jsonwebtoken.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;

import java.sql.*;

@SpringBootApplication
public class SpringSecurityJwtApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityJwtApplication.class, args);
	}

}

@RestController
class HelloWorldController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtTokenUtil;

	@Autowired
	private MyUserDetailsService userDetailsService;

	@RequestMapping({"/hello"})
	public String firstPage() {
		return "Hello World";
	}

	//Users
	@RequestMapping(value = "/users", method = RequestMethod.POST)
	ResponseEntity<String> addUser(@RequestBody User user) {
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		Boolean exists = false;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
			statement = connection.createStatement();
			resultSet = statement.executeQuery("select * from users");
			while (resultSet.next()) {
				if (resultSet.getString(1) == user.getUsername())
					exists = true;
			}
			if (!exists) {
				String query = " insert into users(username,password)"
						+ " values (?, ?)";
				PreparedStatement preparedStmt = connection.prepareStatement(query);
				preparedStmt.setString(1, user.getUsername());
				preparedStmt.setString(2, BCrypt.hashpw(user.getPassword(),BCrypt.gensalt()));
				preparedStmt.execute();
				return new ResponseEntity<String>("User registered", HttpStatus.ACCEPTED);
			}
			connection.close();
		} catch (SQLIntegrityConstraintViolationException e) {
			return new ResponseEntity<>("Incomplete data to register", HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<String>("Username already exists", HttpStatus.ALREADY_REPORTED);
	}
    @RequestMapping(value="/users",method=RequestMethod.GET)
    public ResponseEntity<String> userGET()
    {
        String s="\n";
        Connection connection;
        Statement statement;
        ResultSet resultSet;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
            statement=connection.createStatement();
            resultSet=statement.executeQuery("select * from users");
            while(resultSet.next()){
                s+=resultSet.getString(1)+"\n";
            }
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ResponseEntity<>(("Users found: \n "+s),HttpStatus.OK);
    }
    @RequestMapping(value="/users/{username}",method=RequestMethod.DELETE)
    ResponseEntity<String> deleteUser(@PathVariable("username")String username){
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
                if(resultSet.getString(1).equals(username)) {

                    exists=true;
                    String query = " delete from users where"
                            + " username=?";
                    PreparedStatement preparedStmt = connection.prepareStatement(query);
                    preparedStmt.setString (1, username);
                    preparedStmt.execute();
                    break;
                }
            }
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(exists) {

            return new ResponseEntity<>("User deleted", HttpStatus.ACCEPTED);
        }
        else
            return new ResponseEntity<>("User not found",HttpStatus.NOT_FOUND);

    }
	@RequestMapping(value = "/authenticate", method = RequestMethod.POST)
	public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {

		try {
			if(!BCrypt.checkpw(authenticationRequest.getPassword(),userDetailsService.hashPassword(authenticationRequest.getUsername()))) {
				throw new BadCredentialsException("BAD");
			}
		} catch (BadCredentialsException e) {
			throw new Exception("Incorrect username or password", e);
		}


		final UserDetails userDetails = userDetailsService
				.loadUserByUsername(authenticationRequest.getUsername());

		final String jwt = jwtTokenUtil.generateToken(userDetails);

		return ResponseEntity.ok(new AuthenticationResponse(jwt));
	}

//Tables
@RequestMapping(value="/tables",method = RequestMethod.POST)
public ResponseEntity<String> tablePost(@RequestBody Tables tables){
	Connection connection;
	Statement statement;
	ResultSet resultSet;
	Boolean exists=false;
	try {
		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
		connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban", "root", "");
		statement = connection.createStatement();
		resultSet = statement.executeQuery("select * from tables ");
		while (resultSet.next()) {
			if (resultSet.getInt(1) == tables.getId())
				exists = true;
		}
		if (!exists) {
			String query = " insert into tables(table_id,username)"
					+ " values (?, ?)";
			PreparedStatement preparedStmt = connection.prepareStatement(query);
			preparedStmt.setInt(1, tables.getId());
			preparedStmt.setString(2, tables.getUsername());
			preparedStmt.execute();
			return new ResponseEntity<String>("Table create", HttpStatus.ACCEPTED);
		}
		connection.close();
	} catch (SQLIntegrityConstraintViolationException e) {
		return new ResponseEntity<>("Incomplete data to update", HttpStatus.BAD_REQUEST);
	} catch (Exception e) {
		e.printStackTrace();
	}
	return new ResponseEntity<String>("Table already exists",HttpStatus.ALREADY_REPORTED);

}
	@RequestMapping(value="/tables",method=RequestMethod.GET)
	public ResponseEntity<String> tableGet()
	{
		String s="";
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
			statement=connection.createStatement();
			resultSet=statement.executeQuery("select * from tables ");
			while(resultSet.next()){
				s+=resultSet.getInt(1)+" "+resultSet.getInt(2)+"\n";
			}
			connection.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		return new ResponseEntity<>(("Table found: \n "+s),HttpStatus.OK);
	}

//Lists
@RequestMapping(value="/list",method=RequestMethod.POST)
ResponseEntity<String> postList(@RequestBody Lists lists){
	Connection con;
	Statement st;
	ResultSet rs;
	Boolean exists=false;
	try{
		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
		con= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
		st=con.createStatement();
		rs=st.executeQuery("select * from lists");
		while(rs.next()){
			if(rs.getInt(1)==lists.getId())
				exists=true;
		}
		if(!exists)
		{
			String query = " insert into lists(id,listname,userlistname)"
					+ " values (?, ?,?)";
			PreparedStatement preparedStmt = con.prepareStatement(query);
			preparedStmt.setInt  (1, lists.getId());
			preparedStmt.setString(2,lists.getNameList());
			preparedStmt.setString(3,lists.getUserListName());
			preparedStmt.execute();
			return new ResponseEntity<String>("List created",HttpStatus.ACCEPTED);
		}
		con.close();
	}catch(SQLIntegrityConstraintViolationException e){
		return new ResponseEntity<>("Incomplete data to create list", HttpStatus.BAD_REQUEST);
	}
	catch (Exception e){
		e.printStackTrace();
	}
	return new ResponseEntity<String>("List already exists",HttpStatus.ALREADY_REPORTED);
}


	@RequestMapping(value="/list/{id}",method=RequestMethod.GET)
	public ResponseEntity<String> listsGETWithID(@PathVariable("id") int id){
		String s="";
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		Boolean exists=false;
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
			statement=connection.createStatement();
			resultSet=statement.executeQuery("select * from lists");
			while(resultSet.next()){
				if(resultSet.getInt(1)==id) {
					s = resultSet.getInt(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3) 	 + "\n";
					exists=true;
					break;
				}
			}
			connection.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		if(exists)
			return new ResponseEntity<>(("List found: \n"+s),HttpStatus.ACCEPTED);
		else
			return new ResponseEntity<>("List not found",HttpStatus.NOT_FOUND);
	}

	@RequestMapping(value="/list/{id}",method=RequestMethod.DELETE)
	public ResponseEntity<String> listDelete(@PathVariable("id") int id){
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		Boolean exists=false;
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
			statement=connection.createStatement();
			resultSet=statement.executeQuery("select * from lists");
			while(resultSet.next()){
				if(resultSet.getInt(1)==id) {
					exists=true;
					String query = " delete from lists where"
							+ " id=?";
					PreparedStatement preparedStmt = connection.prepareStatement(query);
					preparedStmt.setInt (1, id);
					preparedStmt.execute();
					break;
				}
			}
			connection.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		if(exists) {

			return new ResponseEntity<>("list deleted", HttpStatus.ACCEPTED);
		}
		else
			return new ResponseEntity<>("list not found",HttpStatus.NOT_FOUND);
	}

	@RequestMapping(value="/list/{id}",method=RequestMethod.PUT)
	public ResponseEntity<String> picturePut(@PathVariable("id") int id,@RequestBody Lists lists){
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		Boolean exists=false;
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
			statement =connection.createStatement();
			resultSet= statement.executeQuery("select * from lists");
			while(resultSet.next()){
				if(resultSet.getInt(1)==id) {
					exists=true;
					String query = " update lists set listname=?,userlistname=? where"
							+ " id=?";
					PreparedStatement preparedStmt = connection.prepareStatement(query);
					preparedStmt.setString(1,lists.getNameList());
					preparedStmt.setString(2,lists.getUserListName());
					preparedStmt.setInt(3,id);
					preparedStmt.execute();
					break;
				}
			}
			connection.close();
		}catch(SQLIntegrityConstraintViolationException e){
			return new ResponseEntity<>("Incomplete data to update", HttpStatus.BAD_REQUEST);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		if(exists) {

			return new ResponseEntity<>("List updated", HttpStatus.ACCEPTED);
		}
		else
			return new ResponseEntity<>("List not found",HttpStatus.NOT_FOUND);
	}

	@RequestMapping(value="/task",method=RequestMethod.POST)
	public ResponseEntity<String> taskPost(@RequestBody Task task){
		Connection connection;
		Statement statement;
		ResultSet resultSet;
		Boolean exists=false;
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
			statement=connection.createStatement();
			resultSet=statement.executeQuery("select * from tasks");
			while(resultSet.next()){
				if(resultSet.getInt(1)==task.getId())
					exists=true;
			}
			if(!exists)
			{
				String query = " insert into tasks (id, nametask, usertaskname, description,status,priority)"
						+ " values (?, ?, ?, ?,?,?)";
				PreparedStatement preparedStmt = connection.prepareStatement(query);
				preparedStmt.setInt (1, task.getId());
				preparedStmt.setString (2, task.getName());
				preparedStmt.setString  (3, task.getUserTaskName());
				preparedStmt.setString(4, task.getDescription());
				preparedStmt.setString(5, task.getPriority());
				preparedStmt.setString(6, task.getStatus());

				preparedStmt.execute();
				return new ResponseEntity<String>("Task create",HttpStatus.ACCEPTED);
			}
			connection.close();
		}catch(SQLIntegrityConstraintViolationException e){
			return new ResponseEntity<>("Incomplete data to update", HttpStatus.BAD_REQUEST);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return new ResponseEntity<String>("Task already exists",HttpStatus.ALREADY_REPORTED);
	}

//	@RequestMapping(value="/task/{id}",method=RequestMethod.PUT)
//	public ResponseEntity<String> taskPut(@PathVariable("id") int id,@RequestBody Task task){
//		Connection connection;
//		Statement statement;
//		ResultSet resultSet;
//		Boolean exists=false;
//		try{
//			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
//			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
//			statement =connection.createStatement();
//			resultSet= statement.executeQuery("select * from tasks");
//			while(resultSet.next()){
//				if(resultSet.getInt(1)==id) {
//					exists=true;
//					String query = " update tasks set Id=?,nameTask=?,id_List=?,Description=?,Status=?,Priority=? where"
//							+ " id=?";
//					PreparedStatement preparedStmt = connection.prepareStatement(query);
//					preparedStmt.setInt (1, task.getId());
//					preparedStmt.setString (2, task.getName());
//					preparedStmt.setInt  (3, task.getIdList());
//					preparedStmt.setString(4, task.getDescription());
//					preparedStmt.setString(5, task.getPriority());
//					preparedStmt.setString(6, task.getStatus());
//
//
//					preparedStmt.execute();
//					break;
//				}
//			}
//			connection.close();
//		}catch(SQLIntegrityConstraintViolationException e){
//			return new ResponseEntity<>("Incomplete data to update", HttpStatus.BAD_REQUEST);
//		}
//		catch (Exception e){
//			e.printStackTrace();
//		}
//		if(exists) {
//
//			return new ResponseEntity<>("Task updated", HttpStatus.ACCEPTED);
//		}
//		else
//			return new ResponseEntity<>("Task not found",HttpStatus.NOT_FOUND);
//	}
//


////Tasks
//@RequestMapping(value="/task/{id}",method=RequestMethod.GET)
//public ResponseEntity<String> taskGETWithID(@PathVariable("id") int id){
//	String s="";
//	Connection connection;
//	Statement statement;
//	ResultSet resultSet;
//	Boolean exists=false;
//	try{
//		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
//		connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
//		statement=connection.createStatement();
//		resultSet=statement.executeQuery("select * from task");
//		while(resultSet.next()){
//			if(resultSet.getInt(1)==id) {
//				s = resultSet.getInt(1) + " " + resultSet.getString(2) + " " + resultSet.getInt(3) + " " + resultSet.getString(4) +resultSet.getString(5)+resultSet.getString(6)+ "\n";
//				exists=true;
//				break;
//			}
//		}
//		connection.close();
//	}catch (Exception e){
//		e.printStackTrace();
//	}
//	if(exists)
//		return new ResponseEntity<>(("Task found: \n"+s),HttpStatus.ACCEPTED);
//	else
//		return new ResponseEntity<>("Task not found",HttpStatus.NOT_FOUND);
//}
//
//	@RequestMapping(value="/task/{id}",method=RequestMethod.DELETE)
//	public ResponseEntity<String> taskDelete(@PathVariable("id") int id){
//		Connection connection;
//		Statement statement;
//		ResultSet resultSet;
//		Boolean exists=false;
//		try{
//			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
//			connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/kanban","root","");
//			statement=connection.createStatement();
//			resultSet=statement.executeQuery("select * from task");
//			while(resultSet.next()){
//				if(resultSet.getInt(1)==id) {
//					exists=true;
//					String query = " delete from task where"
//							+ " id=?";
//					PreparedStatement preparedStmt = connection.prepareStatement(query);
//					preparedStmt.setInt (1, id);
//					preparedStmt.execute();
//					break;
//				}
//			}
//			connection.close();
//		}catch (Exception e){
//			e.printStackTrace();
//		}
//		if(exists) {
//
//			return new ResponseEntity<>("list deleted", HttpStatus.ACCEPTED);
//		}
//		else
//			return new ResponseEntity<>("list not found",HttpStatus.NOT_FOUND);
//	}
//


	@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {
		@Autowired
		private UserDetailsService myUserDetailsService;
		@Autowired
		private JwtRequestFilter jwtRequestFilter;

		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			auth.userDetailsService(myUserDetailsService);
		}

		@Bean
		public PasswordEncoder passwordEncoder() {
			return NoOpPasswordEncoder.getInstance();
		}

		@Override
		@Bean
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		@Override
		protected void configure(HttpSecurity httpSecurity) throws Exception {
			httpSecurity.csrf().disable()
					.authorizeRequests().antMatchers("/authenticate","/users").permitAll().
					anyRequest().authenticated().and().
					exceptionHandling().and().sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
			httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		}

	}}