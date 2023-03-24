package com.ssafy.peelingonion.field.service;

import static com.ssafy.peelingonion.common.ConstValues.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ssafy.peelingonion.field.controller.dto.FieldCreateRequest;
import com.ssafy.peelingonion.field.domain.Field;
import com.ssafy.peelingonion.field.domain.FieldRepository;
import com.ssafy.peelingonion.field.domain.MyField;
import com.ssafy.peelingonion.field.domain.MyFieldRepository;
import com.ssafy.peelingonion.field.domain.Storage;
import com.ssafy.peelingonion.field.domain.StorageRepository;
import com.ssafy.peelingonion.onion.domain.Onion;

import reactor.core.publisher.Mono;

@Service
public class FieldService {
	private final FieldRepository fieldRepository;
	private final MyFieldRepository myFieldRepository;
	private final StorageRepository storageRepository;

	public FieldService(FieldRepository fieldRepository,
		MyFieldRepository myFieldRepository,
		StorageRepository storageRepository) {
		this.fieldRepository = fieldRepository;
		this.myFieldRepository = myFieldRepository;
		this.storageRepository = storageRepository;
	}

	public Field createField(FieldCreateRequest fieldCreateReuqest, Long userId) {
		// Field 객체 생성 후, 저장
		Field field = Field.builder()
			.name(fieldCreateReuqest.getName())
			.isDisabled(Boolean.FALSE)
			.createdAt(Instant.now())
			.build();
		fieldRepository.save(field);
		// myField 객체 생성 후, 저장
		MyField myField = MyField.builder()
			.userId(userId)
			.field(field)
			.build();
		myFieldRepository.save(myField);
		return field;
	}

	public List<Field> readAllFields(Long userId) {
		// userId로 MyField 리스트를 찾기
		List<MyField> myFields = myFieldRepository.findAllByUserId(userId);
		List<Field> fields = new ArrayList<>();
		// 해당 리스트로부터 Field 리스트를 찾기
		for (MyField myField : myFields) {
			Field field = fieldRepository.findById(myField.getField().getId()).orElseThrow();
			// 해당 필드 disalbed 체크하기(false면 리스트에 넣기)
			if (field.getIsDisabled() == Boolean.FALSE) {
				fields.add(field);
			}
		}
		return fields;
	}

	public Field readField(Long fieldId) {
		//**챌린지1 : userId, fieldId에 맞는 중개 테이블이 있는지 확인하고 로직을 진행해보는 것
		//           -> 이 때 userId를 컨트롤러에서 받아와야한다.
		//**챌린지2 : field entity가 없을 때, 예외처리하기
		return fieldRepository.findById(fieldId).get();
	}

	public Field updateField(Long fieldId, String updateName) {
		Field field = fieldRepository.findById(fieldId).get();
		field.setName(updateName);
		return fieldRepository.save(field);
	}

	public void deleteField(Long fieldId) {
		Field field = fieldRepository.findById(fieldId).get();
		field.setIsDisabled(Boolean.TRUE);
	}

	/**
	 * User Service와 통신하여 userId로 nickname을 반환
	 * @param userId
	 * @return
	 */
	public String getNameByUserId(Long userId) {
		try {
			return USER_SERVER_CLIENT.get()
				.uri("/user/" + userId.toString() + "/nickname")
				.retrieve()
				.onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(RuntimeException::new))
				.onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(RuntimeException::new))
				.bodyToMono(String.class)
				.block();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * fieldId에 속한 모든 Onion 객체를 반환
	 * @param fieldId
	 * @return
	 */
	public List<Onion> findOnionListByFieldId(Long fieldId) {
		return storageRepository.findAllByFieldId(fieldId)
			.stream()
			.map(Storage::getOnion)
			.collect(Collectors.toList());
	}
}
