package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-28
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;

    private final UserClient userClient;




    @Override
    public PointsBoardVO queryPointBoardBySeason(PointsBoardQuery query) {
        //判断是否是当前赛季，
        Long season = query.getSeason();
        boolean isCurrent=season==null||season==0;
        //获取redis的key
        LocalDateTime now = LocalDateTime.now();
        String key= RedisConstans.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        //查询我的积分和排名
        PointsBoard myBoard=isCurrent?
                queryMyCurrentBoard(key)://查询当前榜单
                queryMyHistoryBoard(season);//查询历史榜单
        //查询榜单列表
        List<PointsBoard> list=isCurrent?
                queryCurrentBoardListf(key,query.getPageNo(),query.getPageSize()):
                queryHistoryBoardList(query);
        //封装vo
        PointsBoardVO vo=new PointsBoardVO();
        //处理我的信息
        if (myBoard!=null){
            vo.setPoints(myBoard.getPoints());
            vo.setRank(myBoard.getRank());
        }
        //查询用户的信息
        Set<Long> userIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        Map<Long,String> userMap=new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(userDTOS)){
            userMap=userDTOS.stream().collect(Collectors.toMap(UserDTO::getId,UserDTO::getName));
        }
        //封装vo
        List<PointsBoardItemVO> items=new ArrayList<>(list.size());
        for (PointsBoard p:list){
            PointsBoardItemVO v=new PointsBoardItemVO();
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            v.setName(userMap.get(p.getUserId()));
            items.add(v);
        }
        vo.setBoardList(items);
        return vo;
    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        // TODO
        return null;
    }

    private List<PointsBoard> queryCurrentBoardListf(String key, Integer pageNo, Integer pageSize) {
        //计算分页
        int from=(pageNo-1)*pageSize;
        //查询
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, from, from + pageSize - 1);
        if (CollUtils.isEmpty(typedTuples)){
            return CollUtils.emptyList();
        }
        //封装
        int rank=from+1;
        List<PointsBoard> list=new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple:typedTuples){
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId==null||points==null){
                continue;
            }
            PointsBoard p=new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setPoints(points.intValue());
            p.setRank(rank++);
            list.add(p);
        }
        return list;
    }

    private PointsBoard queryMyHistoryBoard(Long season) {
        // TODO
        return null;
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        //绑定key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        //获取当前用户
        Long userId = UserContext.getUser();
        //查询积分
        Double points = ops.score(userId);
        //查询排名
        Long rank = ops.reverseRank(userId);
        //封装返回
        PointsBoard p=new PointsBoard();
        p.setPoints(points==null?0:points.intValue());
        p.setRank(rank==null?0:rank.intValue()+1);
        return p;
    }


}
