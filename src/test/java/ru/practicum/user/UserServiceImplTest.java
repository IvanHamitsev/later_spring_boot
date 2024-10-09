package ru.practicum.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = { "jdbc.url=jdbc:postgresql://localhost:5432/later"})
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Autowired
    private final EntityManager em;
    
    @MockBean
    private final UserRepository repository;

    @MockBean
    @InjectMocks
    private final UserService service;

    @Test
    void testSaveUser() {
        // given
        UserDto userDto = makeUserDto("some@email.com", "Пётр", "Иванов");

        // when
        service.saveUser(userDto);

        // then
        TypedQuery<User> query = em.createQuery("Select u from User u where u.email = :email", User.class);
        User user = query.setParameter("email", userDto.getEmail())
                .getSingleResult();

        assertThat(user.getId(), notNullValue());
        assertThat(user.getFirstName(), equalTo(userDto.getFirstName()));
        assertThat(user.getLastName(), equalTo(userDto.getLastName()));
        assertThat(user.getEmail(), equalTo(userDto.getEmail()));
        assertThat(user.getState(), equalTo(userDto.getState()));
        assertThat(user.getRegistrationDate(), notNullValue());
    }

    @Test
    void testGetAllUsers() {
        // given
        List<UserDto> sourceUsers = List.of(
                makeUserDto("ivan@email", "Ivan", "Ivanov"),
                makeUserDto("petr@email", "Petr", "Petrov"),
                makeUserDto("vasilii@email", "Vasilii", "Vasiliev")
        );

        for (UserDto user : sourceUsers) {
            User entity = UserMapper.mapToNewUser(user);
            em.persist(entity);
        }
        em.flush();

        // when
        List<UserDto> targetUsers = service.getAllUsers();

        // then
        assertThat(targetUsers, hasSize(sourceUsers.size()));
        for (UserDto sourceUser : sourceUsers) {
            assertThat(targetUsers, hasItem( allOf(
                    hasProperty("id", notNullValue()),
                    hasProperty("firstName", equalTo(sourceUser.getFirstName())),
                    hasProperty("lastName", equalTo(sourceUser.getLastName())),
                    hasProperty("email", equalTo(sourceUser.getEmail()))
            )));
        }
    }

    private UserDto makeUserDto(String email, String firstName, String lastName) {
        UserDto dto = new UserDto();
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setState(UserState.ACTIVE);

        return dto;
    }
}