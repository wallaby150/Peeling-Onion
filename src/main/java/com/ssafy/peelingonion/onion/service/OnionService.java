package com.ssafy.peelingonion.onion.service;

import com.ssafy.peelingonion.common.StringToInstant;
import com.ssafy.peelingonion.field.domain.Field;
import com.ssafy.peelingonion.field.domain.FieldRepository;
import com.ssafy.peelingonion.field.domain.Storage;
import com.ssafy.peelingonion.field.domain.StorageRepository;
import com.ssafy.peelingonion.onion.controller.dto.MessageCreateRequest;
import com.ssafy.peelingonion.onion.controller.dto.OnionCreateRequest;
import com.ssafy.peelingonion.onion.domain.*;
import com.ssafy.peelingonion.record.domain.MyRecord;
import com.ssafy.peelingonion.record.domain.MyRecordRepository;
import com.ssafy.peelingonion.record.domain.Record;
import com.ssafy.peelingonion.record.domain.RecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static com.ssafy.peelingonion.common.ConstValues.USER_SERVER_CLIENT;

@Service
public class OnionService {
    private final OnionRepository onionRepository;
    private final SendOnionRepository sendOnionRepository;
    private final RecordRepository recordRepository;
    private final MyRecordRepository myRecordRepository;
    private final MessageRepository messageRepository;
    private final ReceiveOnionRepository receiveOnionRepository;
    private final FieldRepository fieldRepository;
    private final StorageRepository storageRepository;

    public OnionService(OnionRepository onionRepository, SendOnionRepository sendOnionRepository,
                        RecordRepository recordRepository,
                        MyRecordRepository myRecordRepository,
                        MessageRepository messageRepository,
                        ReceiveOnionRepository receiveOnionRepository,
                        FieldRepository fieldRepository,
                        StorageRepository storageRepository) {
        this.onionRepository = onionRepository;
        this.sendOnionRepository = sendOnionRepository;
        this.recordRepository = recordRepository;
        this.myRecordRepository = myRecordRepository;
        this.messageRepository = messageRepository;
        this.receiveOnionRepository = receiveOnionRepository;
        this.fieldRepository = fieldRepository;
        this.storageRepository = storageRepository;
    }

    public void createOnion(OnionCreateRequest onionCreateRequest, Long userId){
        Instant instant = StringToInstant.S2Ins(onionCreateRequest.getGrow_due_date());
        Onion onion = Onion.builder()
                .name(onionCreateRequest.getName())
                .imgSrc(onionCreateRequest.getImg_src())
                .userId(userId)
                .createdAt(Instant.now())
                .latestModify(Instant.now())
                .growDueDate(instant)
                .isDisabled(Boolean.FALSE)
                .isSingle(onionCreateRequest.getIs_single())
                .build();
        Onion newOnion = onionRepository.save(onion);

        List<Long> senderIds = onionCreateRequest.getUser_id_list();
        for(Long senderId : senderIds){
            SendOnion sendOnion = SendOnion.builder()
                    .userId(senderId)
                    .receiverNumber(onionCreateRequest.getReceiver_number())
                    .isSended(Boolean.FALSE)
                    .onion(newOnion)
                    .build();
            sendOnionRepository.save(sendOnion);
        }
        // 양파생성시 받는 상대방에게도 양파를 만든다.
        receiveOnionRepository.save(ReceiveOnion.builder()
                .onion(onion)
                .fromUserId(userId)
                .isReceived(Boolean.FALSE)
                .isBookmarked(Boolean.FALSE)
                .receiverNumber(onionCreateRequest.getReceiver_number())
                .isChecked(Boolean.FALSE)
                .build());
    }

    public List<SendOnion> findSendOnions(Long userId) {
        return sendOnionRepository.findALlByUserIdAndIsSended(userId, Boolean.FALSE);
    }

    public void recordMessage(MessageCreateRequest messageCreateRequest, Long userId) {
        // 나의 녹음, 녹음, 메시지 저장해야한다.
        // 녹음 생성
        Record record = recordRepository.save(Record.builder()
                .createdAt(Instant.now())
                .fileSrc(messageCreateRequest.getFile_src())
                .build());
        // 나의 녹음
        myRecordRepository.save(MyRecord.builder()
                .record(record)
                .userId(userId)
                .build());
        // 양파 기록 갱신
        Onion onion = onionRepository.findById(messageCreateRequest.getId()).get();
        onion.setLatestModify(Instant.now());
        onionRepository.save(onion);
        // 메세지 저장
        messageRepository.save(Message.builder()
                .userId(userId)
                .onion(onionRepository.findById(messageCreateRequest.getId()).get())
                .record(record)
                .createdAt(Instant.now())
                .content(messageCreateRequest.getContent())
                .posRate(messageCreateRequest.getPos_rate())
                .negRate(messageCreateRequest.getNeg_rate())
                .build());
    }

    public void throwOnion(Long onionId){
        Onion onion = onionRepository.findById(onionId).get();
        // getGrowDueDate가 지났다면, 그리고 삭제한 양파가 아니라면 아래의 로직을 실행
        if(onion.getGrowDueDate().isBefore(Instant.now()) && !onion.getIsDisabled()){
            // 양파의 전송일 추가하기
            onion.setSendDate(Instant.now());
            onionRepository.save(onion);
            // 내가 만든 양파에서 해당 양파 전송여부 true
            SendOnion sendOnion = sendOnionRepository.findByOnion(onion);
            sendOnion.setIsSended(Boolean.TRUE);
            sendOnionRepository.save(sendOnion);
            // 내가 받은 양파에서 수신 여부 true
            ReceiveOnion receiveOnion = receiveOnionRepository.findByOnion(onion);
            receiveOnion.setIsReceived(Boolean.TRUE);
            receiveOnionRepository.save(receiveOnion);
        }
    }

    public List<ReceiveOnion> findReceiveOnions(Long userId){
        return receiveOnionRepository.findALlByUserIdAndIsReceivedAndIsChecked(userId, Boolean.TRUE, Boolean.FALSE);
    }

    public Onion findOnionById(Long onionId){
        return onionRepository.findById(onionId).get();
    }

    public ReceiveOnion findReceiveOnionByOnionId(Long onionId) {
        Onion onion = onionRepository.findById(onionId).get();
        ReceiveOnion receiveOnion = receiveOnionRepository.findByOnion(onion);
        receiveOnion.setIsChecked(Boolean.TRUE);
        return receiveOnionRepository.save(receiveOnion);
    }

    public void bookmarkOnion(Long onionId) {
        Onion onion = onionRepository.findById(onionId).get();
        ReceiveOnion receiveOnion = receiveOnionRepository.findByOnion(onion);
        if(receiveOnion.getIsBookmarked()) {
            receiveOnion.setIsBookmarked(Boolean.FALSE);
        } else {
            receiveOnion.setIsBookmarked(Boolean.TRUE);
        }
        receiveOnionRepository.save(receiveOnion);
    }

    public void deleteOnion(Long onionId) {
        Onion onion = onionRepository.findById(onionId).get();
        onion.setIsDisabled(Boolean.TRUE);
        onionRepository.save(onion);
    }

    public void transferOnion(Long fromFId, Long toFId, Long onionId) {
        Storage storage = storageRepository.findByFieldIdAndOnionId(fromFId, onionId).get();
        Field toField = fieldRepository.findById(toFId).get();
        storage.setField(toField);
        storageRepository.save(storage);
    }

    public Message findMessageById(Long messageId){
        return messageRepository.findById(messageId).get();
    }

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
}
