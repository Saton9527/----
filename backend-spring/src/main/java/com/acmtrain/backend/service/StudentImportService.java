package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.StudentImportResultResponse;
import com.acmtrain.backend.entity.StudentInfoEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.StudentInfoRepository;
import com.acmtrain.backend.repository.UserAccountRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentImportService {

    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final Map<String, String> HEADER_ALIASES = createHeaderAliases();
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "账号",
            "密码",
            "姓名",
            "年级",
            "专业",
            "CF账号",
            "ATC账号",
            "CF分数",
            "ATC分数",
            "做题数",
            "积分"
    );

    private final UserAccountRepository userAccountRepository;
    private final StudentInfoRepository studentInfoRepository;

    public StudentImportService(
            UserAccountRepository userAccountRepository,
            StudentInfoRepository studentInfoRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.studentInfoRepository = studentInfoRepository;
    }

    public StudentImportResultResponse importStudents(Long operatorId, MultipartFile file) {
        validateOperator(operatorId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传 Excel 文件");
        }

        List<String> errors = new ArrayList<>();
        int importedCount = 0;
        int createdCount = 0;
        int updatedCount = 0;

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel 文件为空");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel 缺少表头");
            }
            Map<String, Integer> columns = parseHeader(headerRow);

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }

                try {
                    ImportRow importRow = parseRow(row, columns, rowIndex + 1);
                    boolean created = upsertStudent(importRow);
                    importedCount++;
                    if (created) {
                        createdCount++;
                    } else {
                        updatedCount++;
                    }
                } catch (ResponseStatusException ex) {
                    errors.add("第 " + (rowIndex + 1) + " 行: " + ex.getReason());
                } catch (Exception ex) {
                    errors.add("第 " + (rowIndex + 1) + " 行: 导入失败");
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取 Excel 文件失败");
        }

        return new StudentImportResultResponse(
                importedCount,
                createdCount,
                updatedCount,
                errors.size(),
                errors
        );
    }

    public byte[] downloadTemplate(Long operatorId) {
        validateOperator(operatorId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生导入模板");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < TEMPLATE_HEADERS.size(); columnIndex++) {
                Cell cell = headerRow.createCell(columnIndex);
                cell.setCellValue(TEMPLATE_HEADERS.get(columnIndex));
                cell.setCellStyle(headerStyle);
            }

            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("student2026001");
            sampleRow.createCell(1).setCellValue("123456");
            sampleRow.createCell(2).setCellValue("张三");
            sampleRow.createCell(3).setCellValue("2023");
            sampleRow.createCell(4).setCellValue("计算机科学与技术");
            sampleRow.createCell(5).setCellValue("zhangsan_cf");
            sampleRow.createCell(6).setCellValue("zhangsan_atc");
            sampleRow.createCell(7).setCellValue(1650);
            sampleRow.createCell(8).setCellValue(1480);
            sampleRow.createCell(9).setCellValue(180);
            sampleRow.createCell(10).setCellValue(260);

            for (int columnIndex = 0; columnIndex < TEMPLATE_HEADERS.size(); columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
                sheet.setColumnWidth(columnIndex, Math.min(sheet.getColumnWidth(columnIndex) + 1024, 12000));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "生成 Excel 模板失败");
        }
    }

    private void validateOperator(Long operatorId) {
        UserAccountEntity operator = userAccountRepository.findById(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前用户不存在"));
        if (!"coach".equalsIgnoreCase(operator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教练可以导入学生账号");
        }
    }

    private Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = normalize(FORMATTER.formatCellValue(cell));
            String alias = HEADER_ALIASES.get(value);
            if (alias != null) {
                columns.put(alias, cell.getColumnIndex());
            }
        }

        List<String> required = List.of("username", "password", "realName", "grade", "major", "cfHandle");
        List<String> missing = required.stream().filter(key -> !columns.containsKey(key)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel 缺少必要表头: " + String.join(", ", missing));
        }
        return columns;
    }

    private ImportRow parseRow(Row row, Map<String, Integer> columns, int rowNumber) {
        String username = required(row, columns, "username", rowNumber);
        String password = required(row, columns, "password", rowNumber);
        String realName = required(row, columns, "realName", rowNumber);
        String grade = required(row, columns, "grade", rowNumber);
        String major = required(row, columns, "major", rowNumber);
        String cfHandle = required(row, columns, "cfHandle", rowNumber);
        String atcHandle = optional(row, columns, "atcHandle");

        return new ImportRow(
                username,
                password,
                realName,
                grade,
                major,
                cfHandle,
                atcHandle,
                parseInteger(optional(row, columns, "cfRating")),
                parseInteger(optional(row, columns, "atcRating")),
                parseInteger(optional(row, columns, "solvedCount")),
                parseInteger(optional(row, columns, "totalPoints"))
        );
    }

    private boolean upsertStudent(ImportRow importRow) {
        Optional<UserAccountEntity> existingUser = userAccountRepository.findByUsername(importRow.username());
        boolean created = existingUser.isEmpty();

        UserAccountEntity user = existingUser.orElseGet(UserAccountEntity::new);
        if (!created && !"student".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在且不是 student 角色");
        }

        user.setUsername(importRow.username());
        user.setPassword(importRow.password());
        user.setRealName(importRow.realName());
        user.setRole("student");
        UserAccountEntity savedUser = userAccountRepository.save(user);

        StudentInfoEntity studentInfo = studentInfoRepository.findByUserId(savedUser.getId()).orElseGet(StudentInfoEntity::new);
        studentInfo.setUserId(savedUser.getId());
        studentInfo.setRealName(importRow.realName());
        studentInfo.setGrade(importRow.grade());
        studentInfo.setMajor(importRow.major());
        studentInfo.setCfHandle(importRow.cfHandle());
        studentInfo.setAtcHandle(blankToNull(importRow.atcHandle()));
        studentInfo.setCfRating(importRow.cfRating());
        studentInfo.setAtcRating(importRow.atcRating());
        studentInfo.setSolvedCount(importRow.solvedCount());
        studentInfo.setTotalPoints(importRow.totalPoints());
        studentInfoRepository.save(studentInfo);
        return created;
    }

    private String required(Row row, Map<String, Integer> columns, String key, int rowNumber) {
        String value = optional(row, columns, key);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必填字段为空: " + key + " (第 " + rowNumber + " 行)");
        }
        return value.trim();
    }

    private String optional(Row row, Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        return cell == null ? null : FORMATTER.formatCellValue(cell).trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数字字段格式错误: " + value);
        }
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (!FORMATTER.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static Map<String, String> createHeaderAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("username", "username");
        aliases.put("account", "username");
        aliases.put("账号", "username");
        aliases.put("用户名", "username");

        aliases.put("password", "password");
        aliases.put("密码", "password");

        aliases.put("realname", "realName");
        aliases.put("name", "realName");
        aliases.put("姓名", "realName");

        aliases.put("grade", "grade");
        aliases.put("年级", "grade");

        aliases.put("major", "major");
        aliases.put("专业", "major");

        aliases.put("cfhandle", "cfHandle");
        aliases.put("codeforces", "cfHandle");
        aliases.put("codeforcesid", "cfHandle");
        aliases.put("cf", "cfHandle");
        aliases.put("cf账号", "cfHandle");

        aliases.put("atchandle", "atcHandle");
        aliases.put("atcoder", "atcHandle");
        aliases.put("atcoderid", "atcHandle");
        aliases.put("atc", "atcHandle");
        aliases.put("atc账号", "atcHandle");

        aliases.put("cfrating", "cfRating");
        aliases.put("cf分数", "cfRating");

        aliases.put("atcrating", "atcRating");
        aliases.put("atc分数", "atcRating");

        aliases.put("solvedcount", "solvedCount");
        aliases.put("做题数", "solvedCount");

        aliases.put("totalpoints", "totalPoints");
        aliases.put("积分", "totalPoints");
        return aliases;
    }

    private record ImportRow(
            String username,
            String password,
            String realName,
            String grade,
            String major,
            String cfHandle,
            String atcHandle,
            Integer cfRating,
            Integer atcRating,
            Integer solvedCount,
            Integer totalPoints
    ) {
    }
}
