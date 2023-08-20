package SWST.eat_together.matching.algorithm;

import SWST.eat_together.matching.socket.MatchingRequest;
import SWST.eat_together.matching.socket.SendingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Queue;

@RequiredArgsConstructor
@Component
public class MatchingTimeoutChecker {
    private final MatchingAlgorithm matchingAlgorithm;
    private final SendingMessage sendingMessage;
    private static final long WAITING_TIME_THRESHOLD_SECONDS = 10; //해당 초가 지나면 시간 초과로 인식, 기준 완화 실행

    public void handleMatchRequest() {
        Instant currentTime = Instant.now();

        Queue<MatchingRequest> matchQueue = matchingAlgorithm.getMatchQueue();

        for (MatchingRequest request : matchQueue) {
            if (hasRequestExceededWaitingTime(request, currentTime)) {
                // 조건 완화 후 요청 받은 시간 변경 메소드.
                if (easeTheOption(request)) {
                    matchQueue.remove(request);
                    matchQueue.offer(request);
                } else {
                    sendingMessage.failureMessageToFront(request);
                    matchQueue.remove(request);
                }
            }
        }
    }

    private boolean hasRequestExceededWaitingTime(MatchingRequest request, Instant currentTime) {
        return Duration.between(request.getReceivedTimestamp(), currentTime).getSeconds() >= WAITING_TIME_THRESHOLD_SECONDS;
    }

    private boolean easeTheOption(MatchingRequest request) {
        System.out.println("easeTheOption");
        System.out.println("request = " + request);

        if (!request.getMenu().equals("any")) {
            request.setMenu("any");
            return true;
        } else if (!request.getAge().equals("any")) {
            request.setAge("any");
            return true;
        } else if (!request.getGender().equals("any")) {
            request.setGender("any");
            return true;
        } else if (!request.getConversation().equals("any")) {
            request.setConversation("any");
            return true;
        }
        return false;
    }
}