package com.solaria.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;
import com.solaria.notify.model.Device;
import com.solaria.notify.model.Notification;

// Fluxo ponta a ponta contra MongoDB real (localhost:27017), em banco isolado
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.mongodb.uri=mongodb://localhost:27017/solaria_notify_flow_test?uuidRepresentation=standard",
        "spring.mongodb.database=solaria_notify_flow_test",
        "spring.data.mongodb.auto-index-creation=true",
        "app.service-keys.api-core=flow-test-core-key" })
class NotificationFlowTests {

    private static final String SERVICE_KEY = "flow-test-core-key";
    private static final String FID = "fid-teste";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    MongoMappingContext mappingContext;

    @BeforeEach
    void resetDatabase() {
        mongoTemplate.getDb().drop();
        // o drop remove os índices criados no boot; recria a partir das anotações
        MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
        for (Class<?> type : List.of(Device.class, Notification.class)) {
            IndexOperations ops = mongoTemplate.indexOps(type);
            resolver.resolveIndexFor(type).forEach(ops::createIndex);
        }
    }

    @AfterAll
    static void dropDatabase(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.getDb().drop();
    }

    private static RequestPostProcessor user(UUID authId) {
        return jwt().jwt(j -> j.subject(authId.toString()));
    }

    private String dispatchBody(UUID authId, String title) {
        return "{\"authId\":\"" + authId + "\",\"type\":\"NEW_MESSAGE\",\"title\":\"" + title
                + "\",\"body\":\"corpo\",\"data\":{\"chatId\":\"1\"}}";
    }

    private void dispatch(UUID authId, String title) throws Exception {
        mockMvc.perform(post("/internal/notifications")
                .header("X-Service-Key", SERVICE_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(dispatchBody(authId, title)))
                .andExpect(status().isCreated());
    }

    private int unreadCount(UUID authId) throws Exception {
        String json = mockMvc.perform(get("/notify/notifications/unread-count").with(user(authId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.count");
    }

    @Test
    void fullFlow() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        mockMvc.perform(put("/notify/devices/" + FID).with(user(userA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"token-a\",\"platform\":\"ANDROID\",\"appVersion\":\"1.0.0\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/notifications")
                .header("X-Service-Key", SERVICE_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(dispatchBody(userA, "Nova mensagem")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sent").value(1))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.removedDevices").value(0));

        String list = mockMvc.perform(get("/notify/notifications").with(user(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].readAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(list, "$.content[0].id");

        Document stored = mongoTemplate.getCollection("notifications").find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.getString("source_service")).isEqualTo("api-core");
        assertThat(unreadCount(userA)).isEqualTo(1);

        // outro usuário não vê nem altera a notificação
        mockMvc.perform(get("/notify/notifications").with(user(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(patch("/notify/notifications/" + id + "/read").with(user(userB)))
                .andExpect(status().isNotFound());
        assertThat(unreadCount(userA)).isEqualTo(1);

        mockMvc.perform(patch("/notify/notifications/" + id + "/read").with(user(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNotEmpty());
        assertThat(unreadCount(userA)).isEqualTo(0);

        dispatch(userA, "Segunda");
        dispatch(userA, "Terceira");
        assertThat(unreadCount(userA)).isEqualTo(2);

        mockMvc.perform(patch("/notify/notifications/read-all").with(user(userA)))
                .andExpect(status().isNoContent());
        assertThat(unreadCount(userA)).isEqualTo(0);
        assertThat(mongoTemplate.getCollection("notifications").countDocuments(new Document("read_at", null)))
                .isZero();

        // troca de conta: B assume o aparelho e A perde o acesso a ele
        mockMvc.perform(put("/notify/devices/" + FID).with(user(userB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"token-b\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isOk());
        assertThat(mongoTemplate.find(new Query(Criteria.where("authId").is(userA)), Device.class)).isEmpty();
        mockMvc.perform(delete("/notify/devices/" + FID).with(user(userA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalRouteRejectsMissingOrWrongKey() throws Exception {
        mockMvc.perform(post("/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dispatchBody(UUID.randomUUID(), "x")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/notifications")
                .header("X-Service-Key", "errada")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dispatchBody(UUID.randomUUID(), "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void indexesExistInMongo() {
        List<IndexInfo> devices = mongoTemplate.indexOps("devices").getIndexInfo();
        assertThat(devices).anySatisfy(i -> assertThat(i.getIndexFields())
                .extracting(f -> f.getKey()).containsExactly("auth_id"));

        List<IndexInfo> notifications = mongoTemplate.indexOps("notifications").getIndexInfo();
        assertThat(notifications).anySatisfy(i -> {
            assertThat(i.getIndexFields()).extracting(f -> f.getKey()).containsExactly("auth_id", "created_at");
            assertThat(i.getIndexFields().get(1).getDirection().isDescending()).isTrue();
        });
        assertThat(notifications).anySatisfy(i -> {
            assertThat(i.getIndexFields()).extracting(f -> f.getKey()).containsExactly("expire_at");
            assertThat(i.getExpireAfter()).isPresent().get().satisfies(d -> assertThat(d.getSeconds()).isZero());
        });
    }
}
