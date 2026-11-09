package iuh.fit.integration.repository;
import iuh.fit.integration.entity.SocialApprovalPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SocialApprovalPolicyRepository  extends MongoRepository<SocialApprovalPolicy, String> {

}
