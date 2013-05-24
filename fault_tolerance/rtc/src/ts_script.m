function [a0 b0 Max_TS Flag] = ts_script(fname, ureboot, objrecovery, mode, objNum, chk_rcost, chk_scost, chkP)
format long;
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Define Eager or Lazy
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Init
a0 = 0;
b0 = 0;
Max_TS = 0;
Flag = 0;

if (strcmp(mode,'checkpoint'))
    LAZY  = 0;
    EAGER = 0;
    CHECK = 1;
end

if (strcmp(mode,'normal'))
    LAZY  = 0;
    EAGER = 0;
    CHECK = 0;
end

if (strcmp(mode,'lazy'))
    LAZY  = 1;
    EAGER = 0;
    CHECK = 0;
end

if (strcmp(mode,'eager'))
    LAZY  = 0;
    EAGER = 1;
    CHECK = 0;
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Construct input curves
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
if (exist(fname, 'file') == 0)
    Flag = -55;   % no file found
    return;
end
CTMat = load(fname);
thds_num = length(CTMat(:, 1));

P = ones(1, thds_num);
C = P;

P(1,1) = CTMat(1,1);
C(1,1) = CTMat(1,2);
a_In = rtcpjd(P(1,1), 0, 0);

% chkP_index = 0;% checkpoint process index
% chkP_record = 0; % only once
% build all tasks with RM into a_In
for i = 2:1:thds_num
    aa = CTMat(i,1);
    bb = CTMat(i,2);
%     if (chkP_record == 0 && CHECK == 1 && P(1,i) > chkP)
%         P(1,i) = chkP;
%         C(1,i) = chk_scost;
%         a_tmp  = rtcpjd(P(1,i), 0, 0);
%         a_In   = cat(1, a_In, a_tmp);
%         chkP_index = i;
%         chkP_record = 1;
%         i = i+1;
%     end
    P(1,i) = aa;
    C(1,i) = bb;
    a_tmp  = rtcpjd(P(1,i), 0, 0);
    a_In   = cat(1, a_In, a_tmp);
end

% 100% cpu at beginning
b_hat = rtcfs(1);

COST_REBOOT = ureboot;
COST_OBJREC = objrecovery;

if (strcmp(mode,'checkpoint'))
    % when fault happens
    COST_RESTORE_CHK = chk_rcost;
    % when no fault
    COST_SAVING_CHK = chk_scost;
    CHECK_P = rtcpjd(chkP, 0, 0);

    % no big difference to do this after the highest task, as ln:116
%     [a_Out_tmp b_Out_tmp Del_tmp Buf_tmp] = rtcgpc(CHECK_P, b_hat, COST_SAVING_CHK);
%     b_hat = rtcapprox(b_Out_tmp, 0, 0);
end

% Higest prio
if (EAGER == 1 || LAZY == 1 || CHECK == 1)
    if (CHECK == 1)
        b_hat = rtcminus(b_hat, COST_RESTORE_CHK); % restore CheckPoint, ureboot hasb included in the cost!
    else
        b_hat = rtcminus(b_hat, COST_REBOOT);
        
        if (objNum > 0) % objNum is not important to CheckPoint
            if (EAGER == 1)
                b_hat = rtcminus(b_hat, COST_OBJREC*objNum*thds_num); % Eager
            end
            if (LAZY == 1)
                a_In(1) = rtcplus(a_In(1), COST_OBJREC*objNum); % Lazy
            end
        end
    end    
end

% already sorted P
max_Ts = 0;
my_ts = 0;
pos = 0;
[a_Out b_Out Del Buf] = rtcgpc(a_In(1), b_hat, C(1,1));

% checkpointing always cost some ... (not shortest peiod, but highest prio/atomic operation)
if (CHECK == 1)
    qqq =  rtcapprox(b_Out, 0, 0);
    kkkk = CHECK_P;
    if (kkkk(1).hasAperiodicPart == 1)
        kkkk(1).setAperiodicPart([]);
    end
    if (kkkk(2).hasAperiodicPart == 1)
        kkkk(2).setAperiodicPart([]);
    end
    
    [a_Out_tmp b_Out_tmp Del_tmp Buf_tmp] = rtcgpc(CHECK_P, qqq, COST_SAVING_CHK);
    a_Out = a_Out_tmp;
    b_Out = b_Out_tmp;
end

left_upto = b_Out;

window_sz = 40;   % related to the computation window size
maxP = floor(max(P(1,:)));
range_total = maxP * window_sz;

if (maxP < 2000000)   % avoid Java Out of Memory for a huge Period
    for i = 1:1:thds_num
        if (i == 1)
            tmp = left_upto;
        end
        
        fprintf ('%d...\b', i);
        if (mod(i, 10) == 0)
            fprintf ('\n');
        end
        
        for j = 1:1:thds_num
            if (P(1,j) < P(1,i))
                if (j == i-1)   %% use the previous calculated result and instead recalculate
                    if (j > 1)
                        tmp = left_upto(j);
                    end
                    if (EAGER == 1 || LAZY == 1 || CHECK == 1)
                        if (CHECK == 1)
                            tmp = rtcminus(tmp, COST_RESTORE_CHK);   % Restore CheckPoint, including ureboot
                        else
                            tmp = rtcminus(tmp, COST_REBOOT);
                            if (objNum > 0)
                                if (EAGER == 1)
                                    tmp = rtcminus(tmp, COST_OBJREC*objNum*thds_num); % Eager
                                end
                                if (LAZY == 1)
                                    a_In(j) = rtcplus(a_In(j), COST_OBJREC*objNum); % Lazy
                                end
                            end
                        end
                    end
                    
                    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    % do this to avoid the increasing issue
                    % just approximation
                    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    tmp =  rtcapprox(tmp, 0, 0);  
                    kkkk = a_In(j);
                    if (kkkk(1).hasAperiodicPart == 1)
                        kkkk(1).setAperiodicPart([]);
                    end
                    if (kkkk(2).hasAperiodicPart == 1)
                        kkkk(2).setAperiodicPart([]);
                    end
                    
                    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    % Update the demand and service, and save the
                    % calculated left_upto
                    % 2nd term in Eq.19 in paper
                    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    [a_out_tmp b_out_tmp del_tmp buf_tmp] = rtcgpc(a_In(j), tmp,  C(1,j));
                    
%                     if (CHECK == 1)
%                         %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                         % do this to avoid the increasing issue
%                         % just approximation for checkpoint task
%                         %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                         tmp =  rtcapprox(b_out_tmp, 0, 0);
%                         kkkk = CHECK_P;
%                         if (kkkk(1).hasAperiodicPart == 1)
%                             kkkk(1).setAperiodicPart([]);
%                         end
%                         if (kkkk(2).hasAperiodicPart == 1)
%                             kkkk(2).setAperiodicPart([]);
%                         end
%                     
%                         [ck_tmp ck_Out_tmp Del_tmp Buf_tmp] = rtcgpc(CHECK_P, tmp, COST_SAVING_CHK);
%                         a_Out = ck_tmp;
%                         b_out_tmp = ck_Out_tmp;
%                     end
                    left_upto   = cat(1, left_upto, b_out_tmp);
                end
            else
                break;
            end
        end
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%
        %% Find the settling time%
        %%%%%%%%%%%%%%%%%%%%%%%%%%
        ain_tmp = a_In(i);
        new_a_hat = rtcaffine(ain_tmp(1), 1, P(1,i));
        if (i == 1)
            tmp = left_upto;
        else
            tmp = left_upto(i);
        end
        new_b_hat = tmp(2);
        
        tmp_c = rtcminus(new_b_hat, new_a_hat);
        seg_list = tmp_c.segmentsLEQ(range_total);
        list_iter = seg_list.segmentListIterator(range_total);
        
%         plotaxis = [0 10000 0 500];
%         rtcplot(new_b_hat, 'b', new_a_hat, 'r', tmp_c, 'g', plotaxis);
    
        my_ts = 0;
        min_level = -0.00001;
        if (seg_list.min(range_total) < min_level)
            while(list_iter.next == 1)
                if (list_iter.yStart < min_level && list_iter.yEnd > 0)
                    tmp_seg = list_iter.segment;
                    my_ts = tmp_seg.xAt(0);
                end
                %            plotaxis = [0 4000 0 500];
                %            rtcplot(tmp_c, 'r', plotaxis);
            end
            if (my_ts > max_Ts)
                max_Ts = my_ts;
            end
        end
    end
    a0 = new_a_hat;
    b0 = new_b_hat;
end

Max_TS = max_Ts
Flag  = 0;


