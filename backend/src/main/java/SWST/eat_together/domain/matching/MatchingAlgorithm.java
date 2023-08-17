package SWST.eat_together.domain.matching;

import SWST.eat_together.domain.member.Member;
import SWST.eat_together.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class MatchingAlgorithm {
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;


    //spring 서버가 실행되는 시점이 생성되는 큐 matchQueue. 서버 종료시 사라진다.
    private static Queue<MatchRequest> matchQueue = new ArrayBlockingQueue<>(100);

    //새로운 스레드 생성. (시간 제한 처리용) -> 정기적으로 handleMatchRequestPeriodcally() 메서드를 실행하여 매칭 요청을 주기적으로 처리한다.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public static void insertQueue(MatchRequest newRequest){
        System.out.println("***** 들어온 요청을 큐에 넣었습니다. *****");
        matchQueue.offer(newRequest); // 들어온 요청을 큐에다 넣는다.
        System.out.println("현재 큐 상태 = " + matchQueue);
    }

    public void startMatching() {
        MatchService matchService = new MatchService(messagingTemplate);

        System.out.println("***** MatchService.startMatching *****");

        List<MatchRequest> matchedRequests = new ArrayList<>();

        //큐에 있는 요청을 하나씩 빼서 request1에 담고 매칭완료 리스트에 존재하는 요청일시
        //반복문을 빠져나간다.
        for (MatchRequest request1 : matchQueue) {
            if (matchedRequests.contains(request1)) {
                continue;
            }

            System.out.println("[현재 기준 요청] : " + request1);

            List<MatchRequest> overThreeScoreList = new ArrayList<>();

            //큐에 있는 요청을 하나씩 request2에 담고 매칭 가능 요청인지 검증.
            for (MatchRequest request2 : matchQueue) {
                if (checkMatchableRequest(request1, request2)) {
                    //서로의 score 계산.
                    int score = calculateMatchingScore(request1, request2);

                    //request1과 request2의 score가 3 이상일 경우 overThreeScoreList에 추가
                    if (score >= 3) {
                        overThreeScoreList.add(request2);
                        System.out.println(request2.getNickname() + "님의 요청은 score 기준을 넘겼습니다.");
                        System.out.println("overThreeScoreList = " + overThreeScoreList);

                    } else System.out.println(request2.getNickname() + "님의 요청은 score 기준을 넘기지 못했습니다.");
                }
            }

            if (!overThreeScoreList.isEmpty()) {

                // requst1의 people이 any일 경우
                if ("any".equals(request1.getPeople())) {
                    caseOfRequest1PeopleIsAny(matchedRequests, overThreeScoreList, request1);

                } else { // request1의 people 속성이 any가 아닌 경우
                    caseOfRequestPeopleIsNotAny(matchedRequests, overThreeScoreList, request1);
                }
            }
        }

        matchQueue.removeAll(matchedRequests);

        //프론트엔드로 매칭 완료 메시지 전송.
        matchService.completeMessageToFront(matchedRequests);
    }

    private boolean checkMatchableRequest(MatchRequest request1, MatchRequest request2) {
        if(request1 != request2 && //비교하는 두 요청이 같지 않고,
                // 기준 요청의 people이 any이거나 서로의 people이 같고,
                ("any".equals(request1.getPeople()) || request1.getPeople().equals(request2.getPeople())) &&

                //서로의 시간 차이가 한시간 이내이고,
                isWithinOneHour(request1.getStartTime(), request2.getStartTime()) &&

                //서로의 거리가 700m 이내일 경우
                checkDistance(request1.getLatitude(), request1.getLongitude(), request2.getLatitude(), request2.getLongitude())){
            return true;
        }
        return false;
    }

    private void caseOfRequest1PeopleIsAny(List<MatchRequest> matchedRequests, List<MatchRequest> overThreeScoreList, MatchRequest request1){

        matchedRequests.add(request1);

        //가장 빈도수 높은 희망 인원 수 구하기
        String mostFrequentPeople = findMostFrequentPeople(overThreeScoreList);


        // 가장 빈도가 높은 인원 값을 key로 가진 key값만큼의 개수(-1)의요청들을 MathedRequests에 추가한다.
        if (mostFrequentPeople != null) {

            if (mostFrequentPeople.equals("any")){

                if (overThreeScoreList.size() < 4) {
                    System.out.println("mostFrequentPeople은 any이며 overTreeScoreList.size는 " + overThreeScoreList.size() + "입니다.");

                    matchedRequests.addAll(overThreeScoreList);
                    System.out.println("matchedRequests = " + matchedRequests);

                } else matchedRequests.addAll(overThreeScoreList.subList(0 , 3));

            } else {
                int targetMatchCount = Integer.parseInt(mostFrequentPeople) - 1; // 매칭완료 리스트에 추가해야할 인원수

                for (MatchRequest tempMatch : overThreeScoreList) {
                    if (tempMatch.getPeople().equals(mostFrequentPeople) && targetMatchCount > 0) {
                        matchedRequests.add(tempMatch);
                        targetMatchCount--;
                    }
                }
            }
        }
        matchQueue.removeAll(matchedRequests);
    }

    private String findMostFrequentPeople(List<MatchRequest> overThreeScoreList) {
        // overThreeScoreList에 있는 모든 요청중 제일 빈도가 큰 people을 구하는 로직.

        //리스트를 해시맵 형태로 변환한다.
        Map<String, Integer> peopleFrequency = new HashMap<>();
        for (MatchRequest tempMatch : overThreeScoreList) {
            String people = tempMatch.getPeople();
            peopleFrequency.put(people, peopleFrequency.getOrDefault(people, 0) + 1);
        }

        System.out.println("peopleFrequency = " + peopleFrequency);

        String mostFrequentPeople = null;   //만약 2명을 선택한 인원이 3명으로 제일 많았다면 해당 값은 2
        int highestFrequency = 0;           //만약 2명을 선택한 인원이 3명으로 제일 많았다면 해당 값은 3

        // 가장 빈도가 높은 인원 값을 찾는다. (hightestFrequency)
        for (Map.Entry<String, Integer> entry : peopleFrequency.entrySet()) {
            if (entry.getValue() > highestFrequency) {
                mostFrequentPeople = entry.getKey();
                highestFrequency = entry.getValue();
            }
        }
        return mostFrequentPeople;
    }

    private void caseOfRequestPeopleIsNotAny(List<MatchRequest> matchedRequests, List<MatchRequest> overThreeScoreList, MatchRequest request1){
        matchedRequests.add(request1);

        int targetMatchCount = Integer.parseInt(request1.getPeople());

        for (MatchRequest tempMatch : overThreeScoreList) {
            //특정 인원 수와 일치하는 요청들을 찾음.
            if (tempMatch.getPeople().equals(request1.getPeople()) && targetMatchCount > 0) {
                matchedRequests.add(tempMatch);
                targetMatchCount--;
            }
        }
    }

    private int calculateMatchingScore(MatchRequest request1, MatchRequest request2) {
        System.out.println("***** 해당 비교 요청은 최소 충족 요건에 부합합니다. score 계산을 시작합니다. *****");

        int score = 0;

        // request1의 메뉴가 any이거나 서로의 메뉴가 같을 경우 ++
        if (request1.getMenu().equals("any") || request1.getMenu().equals(request2.getMenu())) {
            score++;
        }

        // request1의 age가 any일 경우 ++
        if (request1.getAge().equals("any")) {
            score++;
        } //만약 request1의 age가 peer일 경우 서로의 나이 차이차가 2이하일 경우에만 ++
        else if (request1.getAge().equals("peer")) {
            int ageDifference = calculateAgeDifference(request1.getNickname(), request2.getNickname());
            if (ageDifference <= 2) {
                score++;
            }
        }

        //request1의 gender이 any이면 ++
        if (request1.getGender().equals("any")){
            score++;
        } //혹은 requst1의 gender이 same일경우 request2과 성별이 같을 경우 ++
        else if (request1.getGender().equals("same")){
            if (memberRepository.findByNickname(request1.getNickname()).getGender().equals(memberRepository.findByNickname(request2.getNickname()).getGender())){
                score ++;
            }
        }

        //request1의 conversation이 any이거나 서로의 conversation이 같을 경우 ++
        if (request1.getConversation().equals("any") || request1.getConversation().equals(request2.getConversation())) {
            score++;
        }

        return score;
    }

    private int calculateAgeDifference(String nickname1, String nickname2) {
        Member member1 = memberRepository.findByNickname(nickname1);
        Member member2 = memberRepository.findByNickname(nickname2);

        if (member1 != null && member2 != null) {
            int age1 = calculateAge(member1.getDate());
            int age2 = calculateAge(member2.getDate());

            return Math.abs(age1 - age2);
        }

        return Integer.MAX_VALUE; // Return a high value to avoid unnecessary matches
    }

    private int calculateAge(Date birthDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthDate);
        int birthYear = calendar.get(Calendar.YEAR);

        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);

        return currentYear - birthYear;
    }

    private boolean isWithinOneHour(String time1, String time2) {
        LocalTime startTime1 = LocalTime.parse(time1);
        LocalTime startTime2 = LocalTime.parse(time2);

        Duration duration = Duration.between(startTime1, startTime2).abs();
        return duration.compareTo(Duration.ofHours(1)) <= 0;
    }


    public boolean checkDistance(double lat1, double lon1, double lat2, double lon2) {
        int earthRadiusInMeters = 6371000; // Earth's radius in meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadiusInMeters * c;

        return distance <= 700;
    }



    //시간 경과 관련 메서드들
    @Async
    public void startMatchingAsync() {
        System.out.println("***** MatchService.startMatching *****");

        // 1초마다 handleMatchRequestPeriodically() 호출.
        scheduler.scheduleAtFixedRate(this::handleMatchRequestPeriodically, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::startMatching, 0, 1, TimeUnit.SECONDS);
    }

    private void handleMatchRequestPeriodically() {
        MatchService matchService = new MatchService(messagingTemplate);

        //해당 메소드가 호출된 시점의 시간을 currentTime 변수에 할당
        Instant currentTime = Instant.now();

        for (MatchRequest request : matchQueue) {
            // 만약 10초가 지난 요청일경우
            if (hasRequestExceededWaitingTime(request, currentTime)) {
                // 조건 완화 후 요청 받은 시간 변경 메소드.
                if (easeTheOption(request)) {
                    matchQueue.remove(request);
                    matchQueue.offer(request);
                    //mmatchQueue.add(request);
                } else {
                    matchService.failureMessageToFront(request);
                    matchQueue.remove(request);
                }
            }
        }
    }

    private boolean hasRequestExceededWaitingTime(MatchRequest request, Instant currentTime) {
        // 해당 요청이 들어온 시간과 현재 시간 사이에 10초가 지났는지 검증하는 메소드
        return Duration.between(request.getReceivedTimestamp(), currentTime).getSeconds() >= 10;
    }

    private boolean easeTheOption(MatchRequest request) {
        System.out.println("MatchService.easeTheOption");
        System.out.println("request = " + request);

        if (!request.getMenu().equals("any")) {
            request.setMenu("any");
        } else if (!request.getAge().equals("any")) {
            request.setAge("any");
        } else if (!request.getGender().equals("any")) {
            request.setGender("any");
        } else if (!request.getConversation().equals("any")) {
            request.setConversation("any");
        } else {
            return false;
        }
        return true;
    }
}
