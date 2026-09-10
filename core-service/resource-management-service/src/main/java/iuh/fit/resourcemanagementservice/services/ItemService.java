package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateItemRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateItemRequest;
import iuh.fit.resourcemanagementservice.dtos.response.ItemResponse;
import iuh.fit.resourcemanagementservice.entity.Item;
import iuh.fit.resourcemanagementservice.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final S3Service s3Service;

    /**
     * Thêm mới vật phẩm cứu trợ (Item)
     * Hỗ trợ tải ảnh lên S3 qua MultipartFile hoặc truyền trực tiếp imageUrl
     */
    @Transactional
    public ItemResponse createItem(CreateItemRequest request, MultipartFile image) {
        String imageUrl = request.imageUrl();

        System.out.println("Received imageUrl: " + image);

        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = s3Service.uploadFile(image);

            } catch (IOException e) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Tải ảnh lên S3 thất bại: " + e.getMessage()
                );
            }
        }

        System.out.println("Received imageUrl: " + imageUrl);

        String itemName = request.name().trim();
        Optional<Item> existingOpt = itemRepository.findByNameIgnoreCase(itemName);

        Item itemToSave;
        if (existingOpt.isPresent()) {
            Item existing = existingOpt.get();
            if (Boolean.FALSE.equals(existing.getIsDeleted())) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Vật phẩm với tên này đã tồn tại: " + itemName
                );
            }
            // Tái kích hoạt vật phẩm từng bị xóa mềm trước đó
            existing.setName(itemName);
            existing.setUnit(request.unit());
            if (imageUrl != null) {
                existing.setImageUrl(imageUrl);
            }
            existing.setIsDeleted(false);
            itemToSave = existing;
        } else {
            itemToSave = Item.builder()
                    .name(itemName)
                    .unit(request.unit())
                    .imageUrl(imageUrl)
                    .isDeleted(false)
                    .build();
        }

        Item saved = itemRepository.save(itemToSave);
        return ItemResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin vật phẩm cứu trợ (Update Item)
     */
    @Transactional
    public ItemResponse updateItem(UUID id, UpdateItemRequest request, MultipartFile image) {
        Item existing = itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy vật phẩm với ID: " + id
                ));

        if (request.name() != null && !request.name().isBlank()) {
            String newName = request.name().trim();
            Optional<Item> duplicate = itemRepository.findByNameIgnoreCaseAndIsDeletedFalse(newName);
            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Tên vật phẩm đã tồn tại: " + newName
                );
            }
            existing.setName(newName);
        }

        if (request.unit() != null) {
            existing.setUnit(request.unit());
        }

        if (image != null && !image.isEmpty()) {
            try {
                String uploadedUrl = s3Service.uploadFile(image);
                existing.setImageUrl(uploadedUrl);
            } catch (IOException e) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Tải ảnh lên S3 thất bại: " + e.getMessage()
                );
            }
        } else if (request.imageUrl() != null) {
            existing.setImageUrl(request.imageUrl().trim());
        }

        Item updated = itemRepository.save(existing);
        return ItemResponse.fromEntity(updated);
    }

    /**
     * Xóa mềm vật phẩm (cập nhật isDeleted = true)
     */
    @Transactional
    public void deleteItem(UUID id) {
        Item existing = itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy vật phẩm với ID: " + id
                ));

        existing.setIsDeleted(true);
        itemRepository.save(existing);
    }

    /**
     * Lấy chi tiết thông tin một vật phẩm theo ID
     */
    @Transactional(readOnly = true)
    public ItemResponse getItemById(UUID id) {
        Item existing = itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy vật phẩm với ID: " + id
                ));

        return ItemResponse.fromEntity(existing);
    }
}
