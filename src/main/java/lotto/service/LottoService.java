package lotto.service;

import lotto.dto.LottoesDto;
import lotto.dto.LottoDto;
import lotto.dto.MoneyDto;
import lotto.dto.ResultDto;
import lotto.dto.WinNumbersDto;
import lotto.repository.LottoRepository;
import lotto.util.Consts;
import lotto.util.Ranking;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class LottoService {
    private static final int TWO_LOTTO_CIPHERS = 12;
    private static final double TEMPORARY_MULTIPLICATION_FOR_ROUND = 10.0;
    public static final double PERCENT_UNIT = 100.0;
    private final LottoRepository lottoRepository = new LottoRepository();
    private int paidMoney;

    public LottoesDto purchaseLottoes(MoneyDto moneyDto) {
        paidMoney = moneyDto.amount();
        int lottoesCount = calculateLottoCount(moneyDto.amount());
        List<LottoDto> lottoDtoes = generateLottoes(lottoesCount);
        return new LottoesDto(lottoDtoes);
    }

    private int calculateLottoCount(int money) {
        return money / Consts.SINGLE_LOTTO_PRICE.getValue();
    }

    private List<LottoDto> generateLottoes(int lottoesCount) {
        List<LottoDto> lottoDtos = new ArrayList<>(lottoesCount);
        for (int i = 0; i < lottoesCount; i++) {
            lottoDtos.add(lottoRepository.createLotto());
        }
        return lottoDtos;
    }

    public ResultDto generateResult(WinNumbersDto winNumbersDto) {
        List<LottoDto> lottoDtoes = lottoRepository.findLottoDtoes();

        List<Integer> winNumbers = winNumbersDto.winNumbers();
        int bonusNumber = winNumbersDto.bonusNumber();

        Map<Ranking, Integer> rankingCount = generateRankingCount(lottoDtoes, winNumbers, bonusNumber);
        double benefitRate = calculateBenefitRate(rankingCount);
        return new ResultDto(rankingCount, benefitRate);
    }

    private Map<Ranking, Integer> generateRankingCount(List<LottoDto> lottoDtoes,
                                                       List<Integer> winNumbers, int bonusNumber) {
        Map<Ranking, Integer> rankingCount = new HashMap<>();
        for (LottoDto lottoDto : lottoDtoes) {
            List<Integer> lottoNumbers = lottoDto.numbers();
            int matchedNumberCount = countMatchedNumbers(winNumbers, lottoNumbers);
            boolean isBonusNumberMatched = judgeBonusNumberMatched(bonusNumber, lottoNumbers);
            calculateRankingCount(rankingCount, matchedNumberCount, isBonusNumberMatched);
        }
        return rankingCount;
    }

    private int countMatchedNumbers(List<Integer> winNumbers, List<Integer> lottoNumbers) {
        Set<Integer> matchingValidator = new HashSet<>(lottoNumbers);
        matchingValidator.addAll(winNumbers);
        return TWO_LOTTO_CIPHERS - matchingValidator.size();
    }

    private boolean judgeBonusNumberMatched(int bonusNumber, List<Integer> lottoNumbers) {
        return lottoNumbers.contains(bonusNumber);
    }

    private void calculateRankingCount(Map<Ranking, Integer> rankingCount,
                                       int matchedNumberCount, boolean isBonusNumberMatched) {
        for (Ranking ranking : Ranking.values()) {
            if (ranking.isThisRanking(matchedNumberCount, isBonusNumberMatched)) {
                rankingCount.put(ranking, rankingCount.getOrDefault(ranking, 0) + 1);
                return;
            }
        }
    }

    private double calculateBenefitRate(Map<Ranking, Integer> rankingCount) {
        int totalPrizeBenefit = getTotalPrizeBenefit(rankingCount);
        double benefitRate = totalPrizeBenefit * PERCENT_UNIT / paidMoney;
        return roundOffSecondDecimalPlace(benefitRate);
    }

    private int getTotalPrizeBenefit(Map<Ranking, Integer> rankingCount) {
        int totalPrizeBenefit = 0;
        for (Ranking ranking : rankingCount.keySet()) {
            totalPrizeBenefit += rankingCount.get(ranking) * ranking.getPrizeMoney();
        }
        return totalPrizeBenefit;
    }

    private double roundOffSecondDecimalPlace(double benefitRate) {
        return Math.round(benefitRate * TEMPORARY_MULTIPLICATION_FOR_ROUND)
                / TEMPORARY_MULTIPLICATION_FOR_ROUND;
    }
}