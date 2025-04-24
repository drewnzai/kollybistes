package com.kollybistes.core.api;

import com.kollybistes.common.dtos.TransactionDto;
import com.kollybistes.core.api.swaggerinterfaces.TransactionApi;
import com.kollybistes.core.misc.PaginationRequest;
import com.kollybistes.core.misc.PagingResult;
import com.kollybistes.core.services.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions/")
@AllArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;

    @GetMapping("bitcoin")
    @Override
    public ResponseEntity<PagingResult<TransactionDto>> getBitcoinTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    ){
        final PaginationRequest paginationRequest = new PaginationRequest(page, size, sortField, direction);

        return new ResponseEntity<>(transactionService
                .getBitcoinTransactions(paginationRequest),
                HttpStatus.OK);
    }

    @GetMapping("ethereum")
    @Override
    public ResponseEntity<PagingResult<TransactionDto>> getEthereumTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) Sort.Direction direction
    ){
        final PaginationRequest paginationRequest = new PaginationRequest(page, size, sortField, direction);

        return new ResponseEntity<>(transactionService
                .getBitcoinTransactions(paginationRequest),
                HttpStatus.OK);
    }
}
