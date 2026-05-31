package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.req.GoodsSales;
import com.fish.entity.OrderDetailDO;
import com.fish.entity.OrdersDO;
import com.fish.entity.UserDO;
import com.fish.mapper.OrderDetailMapper;
import com.fish.mapper.OrderMapper;
import com.fish.mapper.UserMapper;
import com.fish.service.ReportService;
import com.fish.service.WorkspaceService;
import com.fish.resp.BusinessDataVO;
import com.fish.resp.OrderReportVO;
import com.fish.resp.SalesTop10ReportVO;
import com.fish.resp.TurnoverReportVO;
import com.fish.resp.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = begin;
        dates.add(cursor);
        while (!cursor.equals(end)) {
            cursor = cursor.plusDays(1);
            dates.add(cursor);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Double turnover = sumCompletedOrderAmount(beginTime, endTime);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cursor = begin;
        dateList.add(cursor);
        while (!cursor.equals(end)) {
            cursor = cursor.plusDays(1);
            dateList.add(cursor);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            newUserList.add(getUserCount(beginTime, endTime));
            totalUserList.add(getUserCount(null, endTime));
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cursor = begin;
        dateList.add(cursor);
        while (!cursor.equals(end)) {
            cursor = cursor.plusDays(1);
            dateList.add(cursor);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            orderCountList.add(getOrderCount(beginTime, endTime, null));
            validOrderCountList.add(getOrderCount(beginTime, endTime, OrdersDO.COMPLETED));
        }

        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);
        Double orderCompletionRate = totalOrderCount != 0 ? validOrderCount.doubleValue() / totalOrderCount : 0.0;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<OrdersDO> completedOrders = orderMapper.selectList(Wrappers.lambdaQuery(OrdersDO.class)
                .eq(OrdersDO::getStatus, OrdersDO.COMPLETED)
                .gt(OrdersDO::getOrderTime, beginTime)
                .lt(OrdersDO::getOrderTime, endTime));

        List<Long> orderIds = completedOrders.stream().map(OrdersDO::getId).collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return SalesTop10ReportVO.builder().nameList("").numberList("").build();
        }

        List<OrderDetailDO> orderDetails = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetailDO.class).in(OrderDetailDO::getOrderId, orderIds));

        List<GoodsSales> goodsSalesDTOList = orderDetails.stream()
                .collect(Collectors.groupingBy(OrderDetailDO::getName, Collectors.summingInt(OrderDetailDO::getNumber)))
                .entrySet().stream()
                .map(entry -> new GoodsSales(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(GoodsSales::getNumber).reversed())
                .limit(10)
                .collect(Collectors.toList());

        String nameList = StringUtils.join(
                goodsSalesDTOList.stream().map(GoodsSales::getName).collect(Collectors.toList()), ",");
        String numberList = StringUtils.join(
                goodsSalesDTOList.stream().map(GoodsSales::getNumber).collect(Collectors.toList()), ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    @Override
    public void exportExcel(HttpServletResponse resp) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("templates/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheetAt(0);
            sheet.getRow(1).getCell(1).setCellValue("时间: " + begin + " ~ " + end);

            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));
                XSSFRow row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            ServletOutputStream out = resp.getOutputStream();
            excel.write(out);
            out.flush();
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        return Math.toIntExact(userMapper.selectCount(Wrappers.lambdaQuery(UserDO.class)
                .gt(beginTime != null, UserDO::getCreateTime, beginTime)
                .lt(endTime != null, UserDO::getCreateTime, endTime)));
    }

    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        return Math.toIntExact(orderMapper.selectCount(Wrappers.lambdaQuery(OrdersDO.class)
                .gt(beginTime != null, OrdersDO::getOrderTime, beginTime)
                .lt(endTime != null, OrdersDO::getOrderTime, endTime)
                .eq(status != null, OrdersDO::getStatus, status)));
    }

    private Double sumCompletedOrderAmount(LocalDateTime beginTime, LocalDateTime endTime) {
        List<OrdersDO> orders = orderMapper.selectList(Wrappers.lambdaQuery(OrdersDO.class)
                .eq(OrdersDO::getStatus, OrdersDO.COMPLETED)
                .gt(OrdersDO::getOrderTime, beginTime)
                .lt(OrdersDO::getOrderTime, endTime));
        return orders.stream()
                .map(OrdersDO::getAmount)
                .filter(amount -> amount != null)
                .mapToDouble(amount -> amount.doubleValue())
                .sum();
    }
}
