//+------------------------------------------------------------------+
//|                                            Robo_DayTrade_B3.mq5  |
//|                                          Expert Advisor Automático |
//|                                                     Versão 1.0    |
//+------------------------------------------------------------------+
#property copyright "ZonIA - Day Trade B3"
#property version   "1.00"
#property description "Robô de Day Trade para B3"
#property description "Lê sinais do servidor e executa automaticamente"

//+------------------------------------------------------------------+
//| PARÂMETROS CONFIGURÁVEIS                                          |
//+------------------------------------------------------------------+
input string   ServidorURL     = "https://raw.githubusercontent.com/Zoni1998/axiom/main/sinais.json";  // URL do servidor de sinais
input double   CapitalInicial  = 100000;     // Capital inicial (demo)
input double   RiscoPorOperacao = 2.0;       // % de risco por operação
input int      TimeoutSegundos = 30;         // Timeout HTTP
input int      IntervaloSegundos = 300;      // Checar sinais a cada 5 min
input bool     AtivarRoboMomentum = true;    // Usar estratégia Momentum
input bool     AtivarRoboReversao = true;    // Usar estratégia Reversão

//+------------------------------------------------------------------+
//| GLOBAIS                                                           |
//+------------------------------------------------------------------+
string ultimoSinal = "";
datetime ultimoCheck = 0;

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit() {
   Print("🤖 Robô Day Trade B3 iniciado!");
   Print("📡 Servidor: ", ServidorURL);
   Print("💰 Capital: R$ ", CapitalInicial);
   Print("⚠️  Risco por operação: ", RiscoPorOperacao, "%");
   return(INIT_SUCCEEDED);
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick() {
   // Só opera em horário comercial
   MqlDateTime dt;
   TimeCurrent(dt);
   if (dt.day_of_week < 1 || dt.day_of_week > 5) return;  // Fim de semana
   if (dt.hour < 10 || dt.hour >= 17) return;              // Fora do horário
   
   // Checar sinais a cada intervalo
   if (TimeCurrent() - ultimoCheck < IntervaloSegundos) return;
   ultimoCheck = TimeCurrent();
   
   // Buscar sinais do servidor
   string sinaisJson = BuscarSinais();
   if (sinaisJson == "") return;
   
   // Processar e executar
   ProcessarSinais(sinaisJson);
}

//+------------------------------------------------------------------+
//| Buscar sinais do servidor                                        |
//+------------------------------------------------------------------+
string BuscarSinais() {
   string headers;
   char data[];
   string result;
   
   int res = WebRequest("GET", ServidorURL, NULL, 0, data, TimeoutSegundos, result, headers);
   
   if (res == -1) {
      int err = GetLastError();
      if (err == 4060) {
         Print("⚠️  URL precisa ser adicionada em: Ferramentas > Opções > Expert Advisors > URLs permitidas");
         Print("   Adicione: ", ServidorURL);
      } else {
         Print("❌ Erro HTTP: ", err);
      }
      return "";
   }
   
   return result;
}

//+------------------------------------------------------------------+
//| Processar sinais e executar ordens                                |
//+------------------------------------------------------------------+
void ProcessarSinais(string json) {
   // Parse simples do JSON
   // Ex: {"sinais":{"PETR4":{"acao":"COMPRAR","forca":3,"preco":40.90}}}
   
   // Listar posições abertas
   int totalPos = PositionsTotal();
   
   for (int i = 0; i < ArraySize(AtivosMonitorados); i++) {
      string ticker = AtivosMonitorados[i];
      string acao = ExtrairAcao(json, ticker);
      
      if (acao == "COMPRAR") {
         AbrirOrdem(ticker, ORDER_TYPE_BUY);
      } else if (acao == "VENDER") {
         AbrirOrdem(ticker, ORDER_TYPE_SELL);
      }
   }
}

//+------------------------------------------------------------------+
//| ATIVOS PARA MONITORAR                                            |
//+------------------------------------------------------------------+
string AtivosMonitorados[] = {
   "PETR4", "VALE3", "ITUB4", "BBDC4", "BBAS3",
   "B3SA3", "WEGE3", "ABEV3", "PRIO3", "CSNA3",
   "GGBR4", "CMIN3", "BPAC11", "RENT3", "RADL3"
};

//+------------------------------------------------------------------+
//| Extrair ação do JSON                                             |
//+------------------------------------------------------------------+
string ExtrairAcao(string &json, string ticker) {
   int pos = StringFind(json, ticker);
   if (pos < 0) return "";
   
   int acaoPos = StringFind(json, "\"acao\"", pos);
   if (acaoPos < 0) return "";
   
   int inicio = StringFind(json, "\"", acaoPos + 8);
   if (inicio < 0) return "";
   int fim = StringFind(json, "\"", inicio + 1);
   if (fim < 0) return "";
   
   return StringSubstr(json, inicio + 1, fim - inicio - 1);
}

//+------------------------------------------------------------------+
//| Extrair preço do JSON                                            |
//+------------------------------------------------------------------+
double ExtrairPreco(string &json, string ticker) {
   int pos = StringFind(json, ticker);
   if (pos < 0) return 0;
   
   int precoPos = StringFind(json, "\"preco\"", pos);
   if (precoPos < 0) return 0;
   
   int inicio = StringFind(json, ":", precoPos) + 1;
   int fim = StringFind(json, ",", inicio);
   if (fim < 0) fim = StringFind(json, "}", inicio);
   
   return StringToDouble(StringSubstr(json, inicio, fim - inicio));
}

//+------------------------------------------------------------------+
//| Extrair força do sinal                                           |
//+------------------------------------------------------------------+
int ExtrairForca(string &json, string ticker) {
   int pos = StringFind(json, ticker);
   if (pos < 0) return 0;
   
   int forcaPos = StringFind(json, "\"forca\"", pos);
   if (forcaPos < 0) return 0;
   
   int inicio = StringFind(json, ":", forcaPos) + 1;
   int fim = StringFind(json, ",", inicio);
   if (fim < 0) fim = StringFind(json, "}", inicio);
   
   return (int)StringToInteger(StringSubstr(json, inicio, fim - inicio));
}

//+------------------------------------------------------------------+
//| Abrir ordem de compra/venda                                      |
//+------------------------------------------------------------------+
void AbrirOrdem(string ticker, ENUM_ORDER_TYPE tipo) {
   // Verificar se já tem posição
   for (int i = 0; i < PositionsTotal(); i++) {
      if (PositionGetSymbol(i) == ticker) {
         // Já tem posição aberta
         return;
      }
   }
   
   // Preparar ordem
   MqlTradeRequest request = {};
   MqlTradeResult result = {};
   
   request.action = TRADE_ACTION_DEAL;
   request.symbol = ticker;
   request.volume = CalcularVolume(ticker);
   request.type = tipo;
   request.deviation = 10;
   
   // Preço
   if (tipo == ORDER_TYPE_BUY) {
      request.price = SymbolInfoDouble(ticker, SYMBOL_ASK);
      request.sl = request.price - CalcularStop(ticker, tipo);
      request.tp = request.price + CalcularTake(ticker, tipo);
   } else {
      request.price = SymbolInfoDouble(ticker, SYMBOL_BID);
      request.sl = request.price + CalcularStop(ticker, tipo);
      request.tp = request.price - CalcularTake(ticker, tipo);
   }
   
   // Executar
   if (OrderSend(request, result)) {
      Print("✅ Ordem executada: ", ticker, " ", EnumToString(tipo));
      Print("   Preço: ", request.price);
      Print("   Stop: ", request.sl, " | Take: ", request.tp);
   } else {
      Print("❌ Erro na ordem ", ticker, ": ", result.retcode);
   }
}

//+------------------------------------------------------------------+
//| Calcular volume (gerenciamento de risco)                         |
//+------------------------------------------------------------------+
double CalcularVolume(string ticker) {
   double saldo = AccountInfoDouble(ACCOUNT_BALANCE);
   double risco = saldo * (RiscoPorOperacao / 100.0);
   double ponto = SymbolInfoDouble(ticker, SYMBOL_TRADE_TICK_VALUE);
   
   if (ponto <= 0) return 0.01;
   
   double volume = risco / (CalcularStop(ticker, ORDER_TYPE_BUY) * ponto * 100);
   volume = NormalizeDouble(volume, 2);
   
   // Limitar volume
   double minVol = SymbolInfoDouble(ticker, SYMBOL_VOLUME_MIN);
   double maxVol = SymbolInfoDouble(ticker, SYMBOL_VOLUME_MAX);
   if (volume < minVol) volume = minVol;
   if (volume > maxVol) volume = maxVol;
   
   return volume;
}

//+------------------------------------------------------------------+
//| Calcular Stop Loss                                               |
//+------------------------------------------------------------------+
double CalcularStop(string ticker, ENUM_ORDER_TYPE tipo) {
   double atr = CalcularATR(ticker);
   if (atr <= 0) atr = SymbolInfoDouble(ticker, SYMBOL_POINT) * 50;
   
   if (tipo == ORDER_TYPE_BUY) {
      return atr * 1.5;  // 1.5x ATR abaixo
   } else {
      return atr * 1.5;  // 1.5x ATR acima
   }
}

//+------------------------------------------------------------------+
//| Calcular Take Profit                                             |
//+------------------------------------------------------------------+
double CalcularTake(string ticker, ENUM_ORDER_TYPE tipo) {
   double atr = CalcularATR(ticker);
   if (atr <= 0) atr = SymbolInfoDouble(ticker, SYMBOL_POINT) * 100;
   
   if (tipo == ORDER_TYPE_BUY) {
      return atr * 3.0;  // 3x ATR (risco:retorno 1:2)
   } else {
      return atr * 3.0;
   }
}

//+------------------------------------------------------------------+
//| Calcular ATR (Average True Range)                                |
//+------------------------------------------------------------------+
double CalcularATR(string ticker) {
   int handle = iATR(ticker, _Period, 14);
   if (handle == INVALID_HANDLE) return 0;
   
   double atr[];
   ArraySetAsSeries(atr, true);
   if (CopyBuffer(handle, 0, 0, 3, atr) < 3) return 0;
   
   IndicatorRelease(handle);
   return atr[0];
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason) {
   Print("🤖 Robô Day Trade B3 encerrado!");
}

//+------------------------------------------------------------------+
//| Trade function                                                   |
//+------------------------------------------------------------------+
void OnTrade() {
   // Monitorar posições
}

//+------------------------------------------------------------------+
//| Timer function                                                   |
//+------------------------------------------------------------------+
void OnTimer() {
   // Backup check
}
//+------------------------------------------------------------------+
