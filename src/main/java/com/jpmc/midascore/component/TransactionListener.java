package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public TransactionListener(UserRepository userRepository,
            TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenTransaction(Transaction transaction) {
        // Log the transaction for debugging purposes
        // logger.info("Received transaction: {}", transaction);

        // Validate recipient and sender information
        Optional<UserRecord> senderOpt = userRepository.findById(transaction.getSenderId());
        Optional<UserRecord> recipientOpt = userRepository.findById(transaction.getRecipientId());

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            logger.warn("Invalid transaction: Sender or Recipient not found");
            return;
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        float senderBalance = sender.getBalance();
        float recipientBalance = recipient.getBalance();
        float transactionAmount = transaction.getAmount();

        if (senderBalance < transactionAmount) {
            logger.warn("Invalid transaction: Inadequate sender balance");
            return;
        }

        sender.setBalance(senderBalance - transactionAmount);
        recipient.setBalance(recipientBalance + transactionAmount);

        userRepository.save(sender); // Save sender
        userRepository.save(recipient); // Save recipient

        TransactionRecord transactionRecord = new TransactionRecord(sender,
                recipient, transactionAmount);
        transactionRecordRepository.save(transactionRecord);

        logger.info("Transaction processed and recorded successfully");
        logger.info("Sender Name: {}, Sender Balance: {}", sender.getName(), sender.getBalance());
        logger.info("Receiver Name: {}, Receiver Balance: {}", recipient.getName(), recipient.getBalance());
    }
}