package com.example.practicejava14;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;

@SpringBootApplication
public class PracticeJava14Application {

	public static void main(String[] args) {
		SpringApplication.run(PracticeJava14Application.class, args);
	}

}

@Component
class Runner {
	private final PeopleService peopleService;
	private final JdbcTemplate template;

	Runner(PeopleService peopleService, JdbcTemplate template) {
		this.peopleService = peopleService;
		this.template = template;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void exercise() throws Exception {
		String initDb = """
				create table people 
				(id bigint auto_increment,
				name varchar(255),
				emotional_state integer,
				primary key (id))""";
		template.update(initDb);

		Person el = this.peopleService.create("Elizabeth", EmotionalState.HAPPY);
		System.out.println(el.name() + " was created !!");
		/*Person xy = new Person(1L, "Name");
		System.out.println(xy);
		System.out.println(xy.name());*/
	}
}

record Person(Long id, String name, Integer emotionalState){
	public Person { //COMPACT CONSTRUCTOR!!!
		this.name = name.toUpperCase();
	}
}

@Service
class PeopleService {
	private final JdbcTemplate jdbcTemplate;
	private final String findByIdSQL =
			"""
			select * from people where ID = ?;
			""";
	private final String insertNewPerson =
   		"""
  		insert into people(name, emotional_state) values (?,?);
		""";
	private final RowMapper<Person> rowMapper =
			(resultSet, i) -> new Person(resultSet.getLong("id"),
					resultSet.getString("name"),
					resultSet.getInt("emotional_state"));

	public PeopleService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Person create(String name, EmotionalState state){
		Integer status = switch (state){
			case MEH -> 0;
			case SAD -> -1;
			case HAPPY -> 1;
		};
		var pscf = new PreparedStatementCreatorFactory(insertNewPerson,
				List.of(
				new SqlParameter(Types.VARCHAR, "name"),
				new SqlParameter(Types.INTEGER, "emotional_state"))){

			{
				super.setReturnGeneratedKeys(true);
				setGeneratedKeysColumnNames("id");
			}
		};
		var psc = pscf.newPreparedStatementCreator(List.of(name, status));
		var keyholder = new GeneratedKeyHolder();
	this.jdbcTemplate.update(psc, keyholder);
	if (keyholder.getKey() instanceof Long id) {
		return findById(id);
	}
	throw new IllegalArgumentException("Could not create " + Person.class.getName() + "!");
	}

	public Person findById(Long id){
		return this.jdbcTemplate.queryForObject(findByIdSQL,new Object[]{id}, rowMapper);
	}
}

enum EmotionalState {
	SAD, HAPPY, MEH
}
