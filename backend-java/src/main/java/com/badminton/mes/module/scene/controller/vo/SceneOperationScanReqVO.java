package com.badminton.mes.module.scene.controller.vo;
import jakarta.validation.constraints.*;import lombok.Data;
/** 工序扫码请求。 @author 刘涵 */
@Data public class SceneOperationScanReqVO { @NotBlank @Size(max=64) private String barcodeValue;@Positive private Long equipmentId; }
