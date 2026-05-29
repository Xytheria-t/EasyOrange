import { useEffect, useState } from 'react';
import { creditApi, type CreditScoreResult } from '@/api/creditApi';
import { CreditScoreCard } from '@/components/ai/CreditScoreCard';
import { RefreshCw, Loader2 } from 'lucide-react';
import '@/styles/main.css';
import '@/components/ai/ai-components.css';

function CreditPage() {
  const [credit, setCredit] = useState<CreditScoreResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRecalculating, setIsRecalculating] = useState(false);

  const loadCredit = async () => {
    setIsLoading(true);
    try {
      const { data } = await creditApi.getMyCredit();
      if (data) {setCredit(data);}
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCredit();
  }, []);

  const handleRecalculate = async () => {
    setIsRecalculating(true);
    try {
      await creditApi.recalculateScore();
      await loadCredit();
    } finally {
      setIsRecalculating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <Loader2 className="animate-spin" size={32} />
      </div>
    );
  }

  if (!credit) {
    return <div className="empty-state">暂无信用数据</div>;
  }

  return (
    <div className="credit-page">
      <div className="credit-page-header">
        <h1>我的信用</h1>
        <button
          className="refresh-btn"
          onClick={handleRecalculate}
          disabled={isRecalculating}
        >
          <RefreshCw size={16} className={isRecalculating ? 'animate-spin' : ''} />
          重新计算
        </button>
      </div>

      <CreditScoreCard credit={credit} />
    </div>
  );
}

export default CreditPage;